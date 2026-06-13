package com.goat.cloud.module.ai.agent.handler;

import com.goat.cloud.module.ai.agent.AgentChatContext;
import com.goat.cloud.module.ai.agent.AgentChatHandler;
import com.goat.cloud.module.ai.entity.AiApiSkill;
import com.goat.cloud.module.ai.entity.AiMcpTool;
import com.goat.cloud.module.ai.mapper.AiApiSkillMapper;
import com.goat.cloud.module.ai.mapper.AiMcpToolMapper;
import com.goat.cloud.module.ai.runtime.AiRuntimeHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具加载：解析 Agent 绑定的 API Skills 和 MCP Tools
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpHandler implements AgentChatHandler {

    private final AiApiSkillMapper apiSkillMapper;
    private final AiMcpToolMapper mcpToolMapper;

    @Override
    public void handle(AgentChatContext ctx) {
        if (ctx.isTerminated()) return;

        String toolIds = ctx.getAgent().getToolIds();
        List<Map<String, Object>> tools = resolveAgentTools(toolIds);
        ctx.setTools(tools);

        log.debug("McpHandler: resolved {} tools", tools.size());
    }

    private List<Map<String, Object>> resolveAgentTools(String toolIds) {
        List<Map<String, Object>> tools = new ArrayList<>();
        if (!StringUtils.hasText(toolIds)) return tools;

        for (String token : toolIds.split("[,，\\s]+")) {
            if (!StringUtils.hasText(token)) continue;

            if (token.startsWith("api:")) {
                Long id = AiRuntimeHelper.toLong(token);
                AiApiSkill skill = safeSelectById(apiSkillMapper, id);
                if (skill != null) tools.add(apiSkillMap(skill));
                continue;
            }
            if (token.startsWith("mcp:")) {
                Long id = AiRuntimeHelper.toLong(token);
                AiMcpTool mcpTool = safeSelectById(mcpToolMapper, id);
                if (mcpTool != null) tools.add(mcpToolMap(mcpTool));
                continue;
            }

            Long id = AiRuntimeHelper.toLong(token);
            if (id != null) {
                AiApiSkill skill = safeSelectById(apiSkillMapper, id);
                if (skill != null) tools.add(apiSkillMap(skill));
                AiMcpTool mcpTool = safeSelectById(mcpToolMapper, id);
                if (mcpTool != null) tools.add(mcpToolMap(mcpTool));
            }
        }
        return tools;
    }

    private Map<String, Object> apiSkillMap(AiApiSkill skill) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("kind", "api-skill");
        map.put("id", skill.getApiSkillId());
        map.put("code", skill.getSkillCode());
        map.put("name", skill.getSkillName());
        map.put("method", skill.getHttpMethod());
        map.put("endpoint", skill.getEndpoint());
        return map;
    }

    private Map<String, Object> mcpToolMap(AiMcpTool tool) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("kind", "mcp-tool");
        map.put("id", tool.getMcpToolId());
        map.put("code", tool.getToolCode());
        map.put("name", tool.getToolName());
        map.put("transportType", tool.getTransportType());
        map.put("endpoint", tool.getEndpoint());
        return map;
    }

    private <T> T safeSelectById(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper, java.io.Serializable id) {
        try { return mapper.selectById(id); }
        catch (Exception ex) { return null; }
    }

    @Override
    public int order() {
        return 40;
    }
}
