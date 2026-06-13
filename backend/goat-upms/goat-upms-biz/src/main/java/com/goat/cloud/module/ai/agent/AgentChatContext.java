package com.goat.cloud.module.ai.agent;

import com.goat.cloud.module.ai.entity.AiAgent;
import com.goat.cloud.module.ai.entity.AiModelConfig;
import com.goat.cloud.module.ai.memory.ShortTermMessage;
import com.goat.cloud.module.ai.runtime.model.RagSearchResponse;
import com.goat.cloud.module.ai.runtime.model.ChatBiAskResponse;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体对话责任链上下文
 * 在各 Handler 间流转并被逐步填充
 */
@Data
public class AgentChatContext {

    // ── 输入（构造时固定）──
    private final Long agentId;
    private final String conversationId;
    private final String content;
    private final Long userId;
    private final Map<String, Object> options;

    // ── 控制流 ──
    private boolean terminated;
    private String errorMessage;

    // ── 由各 Handler 依次填充 ──
    private AiAgent agent;
    private AiModelConfig modelConfig;
    private String systemPrompt;
    private List<Map<String, Object>> tools = new ArrayList<>();
    private List<ShortTermMessage> historyMessages = new ArrayList<>();
    private RagSearchResponse ragResponse;
    private ChatBiAskResponse chatBiResponse;
    private List<Map<String, Object>> toolCalls = new ArrayList<>();
    private List<Map<String, Object>> toolResults = new ArrayList<>();
    private Map<String, Object> runtimeMetadata = new LinkedHashMap<>();

    /**
     * 终止链路并设置错误信息
     */
    public void terminate(String errorMessage) {
        this.terminated = true;
        this.errorMessage = errorMessage;
    }
}
