package com.smartticket.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 提交工单入参。 */
@Data
public class SubmitTicketRequest {

    @NotBlank(message = "请填写工单标题")
    @Size(max = 128, message = "标题过长")
    private String title;

    @NotBlank(message = "请填写问题描述")
    private String content;

    /** 可选图片 URL（逗号分隔）。 */
    private String imageUrl;
}
