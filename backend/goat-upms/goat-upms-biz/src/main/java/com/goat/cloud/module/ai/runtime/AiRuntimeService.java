package com.goat.cloud.module.ai.runtime;

import com.goat.cloud.module.ai.model.request.AiChatRequest;
import com.goat.cloud.module.ai.model.vo.AiChatResponse;
import com.goat.cloud.module.ai.runtime.model.AgentRunRequest;
import com.goat.cloud.module.ai.runtime.model.AgentRunResponse;
import com.goat.cloud.module.ai.runtime.model.ChatBiAskRequest;
import com.goat.cloud.module.ai.runtime.model.ChatBiAskResponse;
import com.goat.cloud.module.ai.runtime.model.RagSearchRequest;
import com.goat.cloud.module.ai.runtime.model.RagSearchResponse;
import com.goat.cloud.module.ai.runtime.model.WorkflowRunRequest;
import com.goat.cloud.module.ai.runtime.model.WorkflowRunResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Thin orchestrator delegating to domain-specific runtime services.
 */
@Service
@RequiredArgsConstructor
public class AiRuntimeService {

    private final AiRagSearchService ragSearchService;
    private final AiChatService chatService;
    private final AiChatBiService chatBiService;
    private final AiAgentService agentService;
    private final AiWorkflowService workflowService;

    public RagSearchResponse search(RagSearchRequest request) {
        return ragSearchService.search(request);
    }

    public AiChatResponse chat(AiChatRequest request) {
        return chatService.chat(request);
    }

    public ChatBiAskResponse askChatBi(ChatBiAskRequest request) {
        return chatBiService.askChatBi(request);
    }

    public AgentRunResponse runAgent(Long agentId, AgentRunRequest request) {
        return agentService.runAgent(agentId, request);
    }

    public WorkflowRunResponse runWorkflow(Long workflowId, WorkflowRunRequest request) {
        return workflowService.runWorkflow(workflowId, request);
    }
}