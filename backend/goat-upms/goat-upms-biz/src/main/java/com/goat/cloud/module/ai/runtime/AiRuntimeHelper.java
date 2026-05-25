package com.goat.cloud.module.ai.runtime;

import com.goat.cloud.module.ai.entity.AiChatBiTable;
import com.goat.cloud.module.ai.entity.AiDocument;
import com.goat.cloud.module.ai.entity.AiKnowledgeBase;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Shared utility helpers for AI runtime services.
 */
final class AiRuntimeHelper {

    private AiRuntimeHelper() {}

    static boolean isMissingTable(RuntimeException ex) {
        for (Throwable current = ex; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message == null) {
                continue;
            }
            String lower = message.toLowerCase(Locale.ROOT);
            if ((lower.contains("relation") || lower.contains("table"))
                    && (lower.contains("does not exist") || lower.contains("doesn't exist"))) {
                return true;
            }
        }
        return false;
    }

    static Long entityId(Object entity) {
        if (entity instanceof AiDocument document) {
            return document.getDocumentId();
        }
        if (entity instanceof AiKnowledgeBase knowledgeBase) {
            return knowledgeBase.getKnowledgeBaseId();
        }
        if (entity instanceof AiChatBiTable table) {
            return table.getTableId();
        }
        return null;
    }

    static String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    static String lower(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }

    static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    static Boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            String text = String.valueOf(value).trim();
            if (text.startsWith("api:") || text.startsWith("mcp:")) {
                text = text.substring(4);
            }
            return StringUtils.hasText(text) ? Long.parseLong(text) : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    static Integer toInteger(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    static int clamp(Integer value, int defaultValue, int min, int max) {
        int safeValue = value == null ? defaultValue : value;
        return Math.max(min, Math.min(max, safeValue));
    }

    static String normalizeText(String text) {
        return text == null ? "" : text.trim();
    }

    static String preview(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String compact = text.replaceAll("\\s+", " ").trim();
        if (compact.length() <= maxLength) {
            return compact;
        }
        return compact.substring(0, maxLength) + "...";
    }
}