package com.goat.cloud.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.goat.cloud.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author wangjubin
 */
@Data
@TableName("ai_feedback")
@EqualsAndHashCode(callSuper = true)
public class AiFeedback extends BaseEntity {

    @TableId(value = "feedback_id", type = IdType.AUTO)
    private Long feedbackId;
    private Long sessionId;
    private Long nodeId;
    private String feedbackType;
    private Integer rating;
    private String correctionJson;
    private String comment;
    private String status;
}
