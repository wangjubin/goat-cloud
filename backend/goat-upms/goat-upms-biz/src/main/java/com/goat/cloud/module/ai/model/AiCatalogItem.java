package com.goat.cloud.module.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wangjubin
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiCatalogItem {

    private String code;
    private String name;
    private String category;
    private String status;
    private String description;
}
