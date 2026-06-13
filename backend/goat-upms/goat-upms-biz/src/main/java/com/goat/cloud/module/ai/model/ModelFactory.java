package com.goat.cloud.module.ai.model;

import com.goat.cloud.module.ai.entity.AiModelConfig;
import com.goat.cloud.module.ai.mapper.AiModelConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 模型工厂（参考 snail-ai ModelFactory）
 * 根据模型配置解析对应的模型实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelFactory {

    private final List<ChatModel> chatModels;
    private final List<EmbeddingModel> embeddingModels;
    private final AiModelConfigMapper modelConfigMapper;

    /**
     * 根据模型配置 ID 获取 ChatModel
     */
    public ChatModel getChatModel(Long modelConfigId) {
        AiModelConfig config = modelConfigMapper.selectById(modelConfigId);
        if (config == null) {
            throw new IllegalArgumentException("Model config not found: " + modelConfigId);
        }
        return getChatModel(config);
    }

    /**
     * 根据模型配置获取 ChatModel
     */
    public ChatModel getChatModel(AiModelConfig config) {
        String modelKey = config.getModelCode();
        return chatModels.stream()
                .filter(m -> m.supports(modelKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No ChatModel implementation found for: " + modelKey));
    }

    /**
     * 根据模型配置 ID 获取 EmbeddingModel
     */
    public EmbeddingModel getEmbeddingModel(Long modelConfigId) {
        AiModelConfig config = modelConfigMapper.selectById(modelConfigId);
        if (config == null) {
            throw new IllegalArgumentException("Model config not found: " + modelConfigId);
        }
        return getEmbeddingModel(config);
    }

    /**
     * 根据模型配置获取 EmbeddingModel
     */
    public EmbeddingModel getEmbeddingModel(AiModelConfig config) {
        String modelKey = config.getModelCode();
        return embeddingModels.stream()
                .filter(m -> m.supports(modelKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No EmbeddingModel implementation found for: " + modelKey));
    }

    /**
     * 获取默认 ChatModel
     */
    public ChatModel getDefaultChatModel() {
        return chatModels.stream().findFirst().orElse(null);
    }

    /**
     * 获取默认 EmbeddingModel
     */
    public EmbeddingModel getDefaultEmbeddingModel() {
        return embeddingModels.stream().findFirst().orElse(null);
    }
}
