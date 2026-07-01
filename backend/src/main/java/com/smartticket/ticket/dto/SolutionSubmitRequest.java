package com.smartticket.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 提交完成入参：解决方案文本（必填）+ 处理截图 URL（可选）。 */
@Data
public class SolutionSubmitRequest {

    @NotBlank(message = "解决方案不能为空")
    @Size(max = 2000, message = "解决方案过长")
    private String solutionText;

    private String imageUrl;
}
