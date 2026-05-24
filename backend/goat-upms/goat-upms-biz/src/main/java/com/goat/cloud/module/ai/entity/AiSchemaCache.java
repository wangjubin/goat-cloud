package com.goat.cloud.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.goat.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * @author wangjubin
 */
@Data
@TableName("ai_schema_cache")
@EqualsAndHashCode(callSuper = true)
public class AiSchemaCache extends BaseEntity {

    @TableId(value = "cache_id", type = IdType.AUTO)
    private Long cacheId;
    private Long datasourceId;
    private String cacheKey;
    private String schemaSnapshot;
    private String sampleQueries;
    private Integer hitCount;
    private LocalDateTime lastHitAt;
    private LocalDateTime expiresAt;
}
