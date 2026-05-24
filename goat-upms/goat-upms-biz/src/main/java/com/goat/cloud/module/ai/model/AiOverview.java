package com.goat.cloud.module.ai.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author wangjubin
 */
@Data
@Builder
public class AiOverview {

    private List<AiCatalogItem> assistants;
    private List<AiCatalogItem> rag;
    private List<AiCatalogItem> chatbi;
    private List<AiCatalogItem> agents;
}
