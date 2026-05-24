package com.goat.cloud.module.ai.runtime;

import com.goat.cloud.common.api.ApiResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author wangjubin
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiRuntimeController {

    private final AiRuntimeService aiRuntimeService;

    @PostMapping("/rag/search")
    public ApiResponse<RagSearchResponse> search(@RequestBody(required = false) RagSearchRequest request) {
        return ApiResponse.success(aiRuntimeService.search(request));
    }

    @PostMapping("/chatbi/ask")
    public ApiResponse<ChatBiAskResponse> askChatBi(@RequestBody(required = false) ChatBiAskRequest request) {
        return ApiResponse.success(aiRuntimeService.askChatBi(request));
    }

    @PostMapping("/agents/{id}/run")
    public ApiResponse<AgentRunResponse> runAgent(@PathVariable Long id, @RequestBody(required = false) AgentRunRequest request) {
        return ApiResponse.success(aiRuntimeService.runAgent(id, request));
    }

    @PostMapping("/workflows/{id}/run")
    public ApiResponse<WorkflowRunResponse> runWorkflow(@PathVariable Long id, @RequestBody(required = false) WorkflowRunRequest request) {
        return ApiResponse.success(aiRuntimeService.runWorkflow(id, request));
    }

    @PostMapping("/chat/runtime")
    public ApiResponse<AiChatResponse> chatRuntime(@RequestBody(required = false) AiChatRequest request) {
        return ApiResponse.success(aiRuntimeService.chat(request));
    }
}
