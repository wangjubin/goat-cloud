package com.goat.cloud.module.ai.model.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.goat.cloud.module.ai.entity.AiModelConfig;
import com.goat.cloud.module.ai.model.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容的 EmbeddingModel 实现
 */
@Slf4j
@Component
public class OpenAiEmbeddingModel implements EmbeddingModel {

    private final Environment environment;
    private AiModelConfig config;
    private int dimensions = 1536;

    public OpenAiEmbeddingModel(Environment environment) {
        this.environment = environment;
    }

    @Override
    public boolean supports(String modelKey) {
        return true;
    }

    @Override
    public void setModelConfig(AiModelConfig config) {
        this.config = config;
    }

    @Override
    public AiModelConfig getModelConfig() {
        return this.config;
    }

    @Override
    public float[] embed(String text) {
        if (config == null) {
            throw new IllegalStateException("Model config not set");
        }

        String apiKey = resolveApiKey(config.getApiKeyRef());
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("API key not resolved for embedding model: " + config.getModelCode());
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", config.getModelCode());
            body.put("input", text);

            JsonNode json = RestClient.create()
                    .post()
                    .uri(completionUrl(config.getEndpoint()))
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode data = json.path("data").path(0).path("embedding");
            if (data.isMissingNode() || !data.isArray()) {
                throw new RuntimeException("Invalid embedding response");
            }

            float[] vector = new float[data.size()];
            for (int i = 0; i < data.size(); i++) {
                vector[i] = data.get(i).floatValue();
            }
            return vector;
        } catch (Exception e) {
            log.error("Embedding model call failed: {}", config.getModelCode(), e);
            throw new RuntimeException("Embedding model call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        for (String text : texts) {
            results.add(embed(text));
        }
        return results;
    }

    @Override
    public int getDimensions() {
        return dimensions;
    }

    private String resolveApiKey(String apiKeyRef) {
        if (!StringUtils.hasText(apiKeyRef)) return null;
        String ref = apiKeyRef.trim();
        if (ref.startsWith("ENV:")) return lookupSecret(ref.substring(4));
        if (ref.startsWith("PROP:")) return environment.getProperty(ref.substring(5));
        if (ref.startsWith("${") && ref.endsWith("}")) return lookupSecret(ref.substring(2, ref.length() - 1));
        if (ref.startsWith("VALUE:")) return ref.substring(6);
        return lookupSecret(ref);
    }

    private String lookupSecret(String name) {
        if (!StringUtils.hasText(name)) return null;
        String value = environment.getProperty(name);
        if (!StringUtils.hasText(value)) value = System.getenv(name);
        return StringUtils.hasText(value) ? value : null;
    }

    private String completionUrl(String endpoint) {
        String trimmed = endpoint.trim();
        while (trimmed.endsWith("/")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        if (trimmed.endsWith("/embeddings")) return trimmed;
        if (trimmed.endsWith("/v1")) return trimmed + "/embeddings";
        return trimmed + "/v1/embeddings";
    }
}
