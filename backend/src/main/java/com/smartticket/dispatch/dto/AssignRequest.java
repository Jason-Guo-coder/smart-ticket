package com.smartticket.dispatch.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 手动指派 / 采纳 AI 建议入参：目标工程师。 */
@Data
public class AssignRequest {

    @NotNull(message = "请选择工程师")
    private Long engineerId;
}
