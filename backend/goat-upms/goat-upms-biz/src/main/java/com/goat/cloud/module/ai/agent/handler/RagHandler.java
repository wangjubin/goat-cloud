package com.goat.cloud.module.ai.agent.handler;

import com.goat.cloud.module.ai.agent.AgentChatContext;
import com.goat.cloud.module.ai.agent.AgentChatHandler;
import com.goat.cloud.module.ai.runtime.AiRagSearchService;
import com.goat.cloud.module.ai.runtime.AiRuntimeHelper;
import com.goat.cloud.module.ai.runtime.model.RagSearchRequest;
import com.goat.cloud.module.ai.runtime.model.RagSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 检索：若 Agent 绑定了知识库，执行检索并注入结果到 prompt
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagHandler implements AgentChatHandler {

    private final AiRagSearchService ragSearchService;

    private static final int DEFAULT_RAG_TOP_K = 5;

    @Override
    public void handle(AgentChatContext ctx) {
        if (ctx.isTerminated()) return;

        String knowledgeBaseIdsStr = ctx.getAgent().getKnowledgeBaseIds();
        if (!StringUtils.hasText(knowledgeBaseIdsStr)) return;

        List<Long> knowledgeBaseIds = parseLongList(knowledgeBaseIdsStr);
        if (knowledgeBaseIds.isEmpty()) return;

        boolean ragEnabled = !Boolean.FALSE.equals(AiRuntimeHelper.toBoolean(ctx.getOptions().get("rag")))
                && !Boolean.FALSE.equals(AiRuntimeHelper.toBoolean(ctx.getOptions().get("useRag")));
        if (!ragEnabled) return;

        if (!StringUtils.hasText(ctx.getContent())) return;

        RagSearchRequest ragRequest = new RagSearchRequest();
        ragRequest.setQuery(ctx.getContent());
        ragRequest.setTopK(AiRuntimeHelper.toInteger(ctx.getOptions().get("topK"), DEFAULT_RAG_TOP_K));
        ragRequest.setIncludeContent(Boolean.TRUE);
        ragRequest.setKnowledgeBaseIds(knowledgeBaseIds);

        RagSearchResponse rag = ragSearchService.search(ragRequest);
        ctx.setRagResponse(rag);

        // 注入 RAG 结果到 system prompt
        String ragContext = ragSearchService.buildRagContext(rag);
        if (StringUtils.hasText(ragContext)) {
            ctx.setSystemPrompt(ctx.getSystemPrompt()
                    + "\n\n## Knowledge Base Reference\n\n"
                    + "The following are relevant materials retrieved from knowledge bases:\n\n"
                    + ragContext
                    + "\nPlease answer the user's question based on the above reference materials.\n");
        }

        log.debug("RagHandler: hitCount={}, knowledgeBaseIds={}", rag.getTotal(), knowledgeBaseIds);
    }

    private List<Long> parseLongList(String value) {
        if (!StringUtils.hasText(value)) return List.of();
        List<Long> result = new ArrayList<>();
        for (String token : value.split("[,，\\s]+")) {
            if (StringUtils.hasText(token)) {
                Long id = AiRuntimeHelper.toLong(token);
                if (id != null) result.add(id);
            }
        }
        return result;
    }

    @Override
    public int order() {
        return 60;
    }
}
