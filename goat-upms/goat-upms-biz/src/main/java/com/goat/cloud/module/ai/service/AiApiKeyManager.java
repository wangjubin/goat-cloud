package com.goat.cloud.module.ai.service;

/**
 * API Key 加密管理接口
 * @author wangjubin
 */
public interface AiApiKeyManager {

    /**
     * 加密并存储API Key
     */
    void encryptAndStore(Long modelId, String rawApiKey);

    /**
     * 解密获取API Key
     */
    String decrypt(Long modelId);

    /**
     * 轮转密钥
     */
    void rotateKey(Long modelId, String newKey);

    /**
     * 验证Key有效性
     */
    boolean validateKey(Long modelId);

    /**
     * 获取解密后的Key（用于实际API调用）
     */
    String getDecryptedKey(Long modelId);
}
