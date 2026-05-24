package com.goat.cloud.common.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * @author wangjubin
 */
@Data
public class PageQuery {

    @Min(1)
    private long pageNum = 1;

    @Min(1)
    @Max(200)
    private long pageSize = 10;
}
