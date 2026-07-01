package com.smartticket.evaluation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 评价入参：星级(必填 1-5) + 标签(可选,逗号分隔) + 评论(可选)。 */
@Data
public class EvaluateRequest {

    @NotNull(message = "请打分")
    @Min(value = 1, message = "评分最低 1 星")
    @Max(value = 5, message = "评分最高 5 星")
    private Integer score;

    @Size(max = 128, message = "标签过长")
    private String tags;

    @Size(max = 512, message = "评论过长")
    private String comment;
}
