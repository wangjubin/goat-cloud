package com.goat.cloud.module.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 内容安全服务
 * 负责输入输出内容过滤，防止敏感信息泄露和不当内容生成
 * 
 * @author wangjubin
 */
@Slf4j
@Service
public class AiContentSafetyService {

    // 敏感词库 (实际项目应该从数据库或配置中心加载)
    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
            // 政治敏感
            "反动", "分裂国家",
            // 暴力恐怖
            "恐怖主义", "极端主义",
            // 色情低俗
            "色情", "淫秽",
            // 违法违规
            "赌博", "毒品",
            // 个人信息
            "身份证号", "银行卡号", "密码"
    );
    
    // 敏感信息正则模式
    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w.]+@[\\w.]+\\.\\w+");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("\\d{17}[\\dXx]");
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("\\d{16,19}");

    /**
     * 检查输入内容安全性
     * @return 检查结果，包含是否安全、风险类型、建议操作
     */
    public SafetyCheckResult checkInput(String content) {
        if (!StringUtils.hasText(content)) {
            return SafetyCheckResult.safe();
        }
        
        // 1. 检查敏感词
        for (String word : SENSITIVE_WORDS) {
            if (content.contains(word)) {
                log.warn("Sensitive word detected in input: {}", word);
                return SafetyCheckResult.unsafe("SENSITIVE_WORD", 
                        "输入包含敏感内容，请修改后重试");
            }
        }
        
        // 2. 检查个人信息泄露
        if (containsPersonalInfo(content)) {
            log.warn("Personal information detected in input");
            return SafetyCheckResult.unsafe("PERSONAL_INFO", 
                    "输入可能包含个人敏感信息，请注意隐私保护");
        }
        
        // 3. 检查内容长度
        if (content.length() > 10000) {
            return SafetyCheckResult.unsafe("CONTENT_TOO_LONG", 
                    "输入内容过长，请精简后重试");
        }
        
        return SafetyCheckResult.safe();
    }

    /**
     * 检查输出内容安全性
     */
    public SafetyCheckResult checkOutput(String content) {
        if (!StringUtils.hasText(content)) {
            return SafetyCheckResult.safe();
        }
        
        // 1. 检查敏感词
        for (String word : SENSITIVE_WORDS) {
            if (content.contains(word)) {
                log.warn("Sensitive word detected in output: {}", word);
                return SafetyCheckResult.unsafe("SENSITIVE_WORD", 
                        "AI生成内容包含敏感信息");
            }
        }
        
        // 2. 检查是否包含个人信息
        if (containsPersonalInfo(content)) {
            log.warn("Personal information detected in output");
            return SafetyCheckResult.unsafe("PERSONAL_INFO", 
                    "AI生成内容可能包含个人敏感信息");
        }
        
        return SafetyCheckResult.safe();
    }

    /**
     * 脱敏处理：替换个人信息
     */
    public String desensitize(String content) {
        if (!StringUtils.hasText(content)) {
            return content;
        }
        
        String result = content;
        
        // 替换手机号
        result = PHONE_PATTERN.matcher(result).replaceAll("[手机号已隐藏]");
        
        // 替换邮箱
        result = EMAIL_PATTERN.matcher(result).replaceAll("[邮箱已隐藏]");
        
        // 替换身份证号
        result = ID_CARD_PATTERN.matcher(result).replaceAll("[身份证号已隐藏]");
        
        // 替换银行卡号
        result = BANK_CARD_PATTERN.matcher(result).replaceAll("[银行卡号已隐藏]");
        
        return result;
    }

    /**
     * 检查是否包含个人信息
     */
    private boolean containsPersonalInfo(String content) {
        return PHONE_PATTERN.matcher(content).find() ||
               EMAIL_PATTERN.matcher(content).find() ||
               ID_CARD_PATTERN.matcher(content).find() ||
               BANK_CARD_PATTERN.matcher(content).find();
    }

    /**
     * 安全检查结果
     */
    public static class SafetyCheckResult {
        private final boolean safe;
        private final String riskType;
        private final String message;
        
        private SafetyCheckResult(boolean safe, String riskType, String message) {
            this.safe = safe;
            this.riskType = riskType;
            this.message = message;
        }
        
        public static SafetyCheckResult safe() {
            return new SafetyCheckResult(true, null, null);
        }
        
        public static SafetyCheckResult unsafe(String riskType, String message) {
            return new SafetyCheckResult(false, riskType, message);
        }
        
        public boolean isSafe() {
            return safe;
        }
        
        public String getRiskType() {
            return riskType;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
