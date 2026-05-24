package com.goat.cloud.module.system.model.request;

import com.goat.cloud.common.enums.CommonStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author wangjubin
 */
@Data
public class StatusChangeRequest {

    @NotNull
    private Long id;

    @NotNull
    private CommonStatus status;
}
