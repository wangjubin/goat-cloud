package com.goat.cloud.module.ai.runtime.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author wangjubin
 */
@Data
public class RagSearchResponse {

    private String query;
    private Integer total;
    private List<RagSearchHit> hits;
    private Map<String, Object> metadata;
    private LocalDateTime searchedAt;
}
