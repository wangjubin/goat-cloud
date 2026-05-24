package com.goat.cloud.module.ai.service.stategraph;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * 自动注册所有 NodeExecutor 到 StateExecutionEngine
 * @author wangjubin
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class NodeExecutorRegistry {

    private final StateExecutionEngine executionEngine;
    private final List<NodeExecutor> executors;

    @PostConstruct
    public void registerAll() {
        for (NodeExecutor executor : executors) {
            executionEngine.registerExecutor(executor);
            log.info("Auto-registered node executor: {} ({})",
                    executor.getNodeType(), executor.getClass().getSimpleName());
        }
        log.info("Total {} node executors registered", executors.size());
    }
}