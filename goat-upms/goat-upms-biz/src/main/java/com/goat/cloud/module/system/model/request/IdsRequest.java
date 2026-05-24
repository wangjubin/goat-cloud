package com.goat.cloud.module.system.model.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * @author wangjubin
 */
@Data
public class IdsRequest {

    @NotEmpty
    private List<Long> ids;
}
