package com.goat.cloud.module.ai.service.stategraph;

/**
 * 节点执行结果
 * @author wangjubin
 */
public record NodeResult(
        boolean success,
        String outputJson,
        String errorMessage,
        boolean shouldInterrupt
) {

    public static NodeResult ok(String outputJson) {
        return new NodeResult(true, outputJson, null, false);
    }

    public static NodeResult interrupt(String outputJson) {
        return new NodeResult(true, outputJson, null, true);
    }

    public static NodeResult fail(String errorMessage) {
        return new NodeResult(false, null, errorMessage, false);
    }
}
