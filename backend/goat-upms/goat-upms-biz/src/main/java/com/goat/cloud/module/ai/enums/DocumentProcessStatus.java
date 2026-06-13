package com.goat.cloud.module.ai.enums;

/**
 * 文档处理状态枚举
 */
public enum DocumentProcessStatus {

    PENDING("PENDING", "等待处理"),
    PROCESSING("PROCESSING", "处理中"),
    SUCCESS("SUCCESS", "处理成功"),
    FAILED("FAILED", "处理失败");

    private final String status;
    private final String description;

    DocumentProcessStatus(String status, String description) {
        this.status = status;
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public static DocumentProcessStatus fromValue(String value) {
        for (DocumentProcessStatus status : values()) {
            if (status.status.equals(value)) {
                return status;
            }
        }
        return null;
    }
}
