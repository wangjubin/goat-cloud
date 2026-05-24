package com.goat.cloud.module.ai.service.impl;

import com.goat.cloud.module.ai.entity.AiModelConfig;
import com.goat.cloud.module.ai.mapper.AiModelConfigMapper;
import com.goat.cloud.module.ai.service.AiApiKeyManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * API Key 加密管理实现
 * @author wangjubin
 */
@Slf4j
@Service
public class AiApiKeyManagerImpl implements AiApiKeyManager {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    @Value("${goat.ai.encryption.master-key:please-change-aes-master-key-32ch!}")
    private String masterKey;

    @Resource
    private AiModelConfigMapper modelConfigMapper;

    @Override
    public void encryptAndStore(Long modelId, String rawApiKey) {
        try {
            AiModelConfig config = modelConfigMapper.selectById(modelId);
            if (config == null) {
                throw new IllegalArgumentException("Model not found: " + modelId);
            }

            String encrypted = encrypt(rawApiKey);
            config.setApiKeyEncrypted(encrypted);
            config.setApiKeyVersion("v1");
            modelConfigMapper.updateById(config);

            log.info("API key encrypted and stored for model: {}", modelId);
        } catch (Exception e) {
            log.error("Failed to encrypt and store API key for model: {}", modelId, e);
            throw new RuntimeException("Failed to encrypt API key", e);
        }
    }

    @Override
    public String decrypt(Long modelId) {
        AiModelConfig config = modelConfigMapper.selectById(modelId);
        if (config == null || config.getApiKeyEncrypted() == null) {
            return null;
        }
        try {
            return decrypt(config.getApiKeyEncrypted());
        } catch (Exception e) {
            log.error("Failed to decrypt API key for model: {}", modelId, e);
            return null;
        }
    }

    @Override
    public void rotateKey(Long modelId, String newKey) {
        encryptAndStore(modelId, newKey);
        log.info("API key rotated for model: {}", modelId);
    }

    @Override
    public boolean validateKey(Long modelId) {
        try {
            String key = getDecryptedKey(modelId);
            return key != null && !key.isBlank();
        } catch (Exception e) {
            log.warn("API key validation failed for model: {}", modelId, e);
            return false;
        }
    }

    @Override
    public String getDecryptedKey(Long modelId) {
        return decrypt(modelId);
    }

    private String encrypt(String plaintext) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        SecretKeySpec keySpec = new SecretKeySpec(
                padKey(masterKey).getBytes(StandardCharsets.UTF_8), "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // Combine IV + encrypted data
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    private String decrypt(String encrypted) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encrypted);

        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
        System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

        SecretKeySpec keySpec = new SecretKeySpec(
                padKey(masterKey).getBytes(StandardCharsets.UTF_8), "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

        byte[] decrypted = cipher.doFinal(cipherText);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private String padKey(String key) {
        if (key.length() >= 32) {
            return key.substring(0, 32);
        }
        return String.format("%-32s", key).substring(0, 32);
    }
}
