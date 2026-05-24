package com.goat.cloud.module.ai.service.stategraph.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goat.cloud.module.ai.entity.AiPythonConfig;
import com.goat.cloud.module.ai.mapper.AiPythonConfigMapper;
import com.goat.cloud.module.ai.service.stategraph.NodeExecutor;
import com.goat.cloud.module.ai.service.stategraph.NodeResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Python 执行节点执行器
 * <p>
 * 支持在 Docker 或本地进程中执行 Python 代码进行数据分析
 * @author wangjubin
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PythonExecutionExecutor implements NodeExecutor {

    private final AiPythonConfigMapper pythonConfigMapper;
    private final ObjectMapper objectMapper;

    @Override
    public String getNodeType() {
        return "PYTHON_EXECUTION";
    }

    @Override
    public NodeResult execute(Map<String, Object> context, String nodeConfig) {
        try {
            // 获取 Python 执行配置
            AiPythonConfig config = pythonConfigMapper.selectOne(
                    new LambdaQueryWrapper<AiPythonConfig>()
                            .eq(AiPythonConfig::getStatus, "ENABLED")
                            .last("limit 1")
            );

            if (config == null) {
                return NodeResult.fail("No Python execution config available");
            }

            // 从上下文中获取代码
            String pythonCode = extractPythonCode(context, nodeConfig);
            if (pythonCode == null || pythonCode.isBlank()) {
                return NodeResult.fail("No Python code to execute");
            }

            // 执行 Python 代码
            Map<String, Object> result;
            if ("DOCKER".equals(config.getExecutionMode())) {
                result = executeInDocker(config, pythonCode);
            } else {
                result = executeLocal(config, pythonCode);
            }

            context.put("pythonResult", result);
            return NodeResult.ok(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("Python execution error", e);
            return NodeResult.fail("Python execution failed: " + e.getMessage());
        }
    }

    private String extractPythonCode(Map<String, Object> context, String nodeConfig) {
        // 优先从节点配置获取代码模板
        if (nodeConfig != null && !nodeConfig.isBlank()) {
            try {
                Map<String, Object> config = objectMapper.readValue(nodeConfig, new com.fasterxml.jackson.core.type.TypeReference<>() {});
                Object code = config.get("pythonCode");
                if (code != null && !code.toString().isBlank()) {
                    return code.toString();
                }
            } catch (Exception ignored) {}
        }

        // 否则生成默认的数据分析代码
        Object sqlResult = context.get("sqlResult");
        if (sqlResult != null) {
            return generateDataAnalysisCode(context);
        }

        return null;
    }

    private String generateDataAnalysisCode(Map<String, Object> context) {
        return """
                import pandas as pd
                import json
                import sys

                # 从标准输入获取数据
                data = json.loads(sys.stdin.read())
                df = pd.DataFrame(data.get('rows', []))

                # 基本统计分析
                result = {
                    'shape': list(df.shape),
                    'columns': list(df.columns),
                    'dtypes': {col: str(dt) for col, dt in df.dtypes.items()},
                    'describe': json.loads(df.describe().to_json()) if not df.empty else {},
                    'head': json.loads(df.head(5).to_json(orient='records'))
                }
                print(json.dumps(result, ensure_ascii=False))
                """;
    }

    private Map<String, Object> executeLocal(AiPythonConfig config, String pythonCode) throws Exception {
        // 写入临时文件
        Path tempFile = Files.createTempFile("chatbi_", ".py");
        try {
            Files.writeString(tempFile, pythonCode);

            ProcessBuilder pb = new ProcessBuilder("python3", tempFile.toString());
            pb.redirectErrorStream(true);
            int timeout = config.getTimeoutSeconds() != null ? config.getTimeoutSeconds() : 60;
            pb.environment().put("PYTHONIOENCODING", "utf-8");

            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            boolean completed = process.waitFor(timeout, java.util.concurrent.TimeUnit.SECONDS);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("mode", "local");
            result.put("exitCode", completed ? process.exitValue() : -1);
            result.put("output", output);
            result.put("timeout", !completed);
            return result;
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private Map<String, Object> executeInDocker(AiPythonConfig config, String pythonCode) throws Exception {
        String image = config.getDockerImage() != null ? config.getDockerImage() : "python:3.11-slim";

        ProcessBuilder pb = new ProcessBuilder(
                "docker", "run", "--rm", "-i",
                "--memory=" + (config.getMemoryLimitMb() != null ? config.getMemoryLimitMb() : 512) + "m",
                image, "python", "-c", pythonCode
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes());
        int timeout = config.getTimeoutSeconds() != null ? config.getTimeoutSeconds() : 60;
        boolean completed = process.waitFor(timeout, java.util.concurrent.TimeUnit.SECONDS);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", "docker");
        result.put("image", image);
        result.put("exitCode", completed ? process.exitValue() : -1);
        result.put("output", output);
        result.put("timeout", !completed);
        return result;
    }
}