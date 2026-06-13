package com.goat.cloud.module.ai.memory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 短期记忆消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShortTermMessage {
    private String role;
    private String content;
}
