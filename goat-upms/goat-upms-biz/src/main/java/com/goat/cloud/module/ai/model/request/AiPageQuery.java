package com.goat.cloud.module.ai.model.request;

import com.goat.cloud.common.web.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author wangjubin
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiPageQuery extends PageQuery {

    private String keyword;
    private String status;
}
