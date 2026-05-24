package com.goat.cloud.module.ai.service.impl;

import com.goat.cloud.module.ai.entity.AiModelConfig;
import com.goat.cloud.module.ai.entity.AiVectorConfig;
import com.goat.cloud.module.ai.mapper.AiVectorConfigMapper;
import com.goat.cloud.module.ai.service.AiEmbeddingService;
import com.goat.cloud.module.ai.service.AiModelRouter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 向量化服务实现
 * @author wangjubin
 */
@Slf4j
@Service
public class AiEmbeddingServiceImpl implements AiEmbeddingService {

    @Value("${ai.embedding.default-dimension:1536}")
    private int defaultDimension;

    @Value("${ai.embedding.api-timeout:30000}")
    private int apiTimeout;

    @Resource
    private AiVectorConfigMapper vectorConfigMapper;

    @Resource
    private AiModelRouter modelRouter;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public float[] generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return new float[defaultDimension];
        }

        AiVectorConfig vectorConfig = getActiveVectorConfig();
        AiModelConfig embeddingModel = getEmbeddingModel(vectorConfig);

        if (embeddingModel == null) {
            log.warn("No embedding model available, returning zero vector");
            return new float[defaultDimension];
        }

        try {
            return callEmbeddingApi(embeddingModel, text);
        } catch (Exception e) {
            log.error("Failed to generate embedding", e);
            return new float[defaultDimension];
        }
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        AiVectorConfig vectorConfig = getActiveVectorConfig();
        AiModelConfig embeddingModel = getEmbeddingModel(vectorConfig);

        if (embeddingModel == null) {
            log.warn("No embedding model available, returning zero vectors");
            return texts.stream()
                    .map(t -> new float[defaultDimension])
                    .toList();
        }

        try {
            return callEmbeddingsBatchApi(embeddingModel, texts);
        } catch (Exception e) {
            log.error("Failed to generate embeddings batch", e);
            return texts.stream()
                    .map(t -> new float[defaultDimension])
                    .toList();
        }
    }

    @Override
    public int getEmbeddingDimension() {
        AiVectorConfig vectorConfig = getActiveVectorConfig();
        if (vectorConfig != null && vectorConfig.getEmbeddingDimension() != null) {
            return vectorConfig.getEmbeddingDimension();
        }
        return defaultDimension;
    }

    @Override
    public double cosineSimilarity(float[] vec1, float[] vec2) {
        if (vec1 == null || vec2 == null || vec1.length != vec2.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        double denominator = Math.sqrt(norm1) * Math.sqrt(norm2);
        if (denominator == 0.0) {
            return 0.0;
        }

        return dotProduct / denominator;
    }

    private AiVectorConfig getActiveVectorConfig() {
        List<AiVectorConfig> configs = vectorConfigMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiVectorConfig>()
                        .eq(AiVectorConfig::getStatus, com.goat.cloud.common.enums.CommonStatus.ENABLED)
                        .last("LIMIT 1")
        );
        return configs.isEmpty() ? null : configs.get(0);
    }

    private AiModelConfig getEmbeddingModel(AiVectorConfig vectorConfig) {
        if (vectorConfig != null && vectorConfig.getEmbeddingModel() != null) {
            return modelRouter.routeByCapability("embedding");
        }
        return modelRouter.routeByType("embedding");
    }

    private float[] callEmbeddingApi(AiModelConfig model, String text) {
        String endpoint = model.getEndpoint();
        String apiKey = model.getApiKeyRef();

        if (endpoint == null || endpoint.isBlank()) {
            log.warn("Embedding endpoint not configured, returning mock embedding");
            return generateMockEmbedding(text);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (apiKey != null && !apiKey.isBlank()) {
                headers.setBearerAuth(apiKey);
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("input", text);
            requestBody.put("model", model.getModelCode());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String apiUrl = endpoint + (endpoint.endsWith("/") ? "" : "/") + "embeddings";

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(apiUrl, request, Map.class);

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
                if (!dataList.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> embeddingData = dataList.get(0);
                    @SuppressWarnings("unchecked")
                    List<Number> embedding = (List<Number>) embeddingData.get("embedding");
                    if (embedding != null) {
                        float[] result = new float[embedding.size()];
                        for (int i = 0; i < embedding.size(); i++) {
                            result[i] = embedding.get(i).floatValue();
                        }
                        return result;
                    }
                }
            }

            return generateMockEmbedding(text);
        } catch (Exception e) {
            log.error("Failed to call embedding API, using mock: {}", e.getMessage());
            return generateMockEmbedding(text);
        }
    }

    private List<float[]> callEmbeddingsBatchApi(AiModelConfig model, List<String> texts) {
        String endpoint = model.getEndpoint();
        String apiKey = model.getApiKeyRef();

        if (endpoint == null || endpoint.isBlank()) {
            log.warn("Embedding endpoint not configured, returning mock embeddings");
            return texts.stream().map(this::generateMockEmbedding).toList();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (apiKey != null && !apiKey.isBlank()) {
                headers.setBearerAuth(apiKey);
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("input", texts);
            requestBody.put("model", model.getModelCode());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String apiUrl = endpoint + (endpoint.endsWith("/") ? "" : "/") + "embeddings";

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(apiUrl, request, Map.class);

            List<float[]> results = new ArrayList<>();
            int dimension = getEmbeddingDimension();

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.get("data");
                for (Map<String, Object> embeddingData : dataList) {
                    @SuppressWarnings("unchecked")
                    List<Number> embedding = (List<Number>) embeddingData.get("embedding");
                    if (embedding != null) {
                        float[] vec = new float[embedding.size()];
                        for (int i = 0; i < embedding.size(); i++) {
                            vec[i] = embedding.get(i).floatValue();
                        }
                        results.add(vec);
                    } else {
                        results.add(new float[dimension]);
                    }
                }
            }

            while (results.size() < texts.size()) {
                results.add(new float[dimension]);
            }

            return results;
        } catch (Exception e) {
            log.error("Failed to call batch embedding API, using mock: {}", e.getMessage());
            return texts.stream().map(this::generateMockEmbedding).toList();
        }
    }

    private float[] generateMockEmbedding(String text) {
        int dimension = getEmbeddingDimension();
        float[] embedding = new float[dimension];
        Random random = new Random(text.hashCode());

        for (int i = 0; i < dimension; i++) {
            embedding[i] = (random.nextFloat() * 2) - 1;
        }

        normalize(embedding);
        return embedding;
    }

    private void normalize(float[] vector) {
        double norm = 0.0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) (vector[i] / norm);
            }
        }
    }
}
