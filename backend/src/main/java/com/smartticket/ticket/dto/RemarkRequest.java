package com.smartticket.ticket.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 通用备注入参（转派原因 / 验收驳回原因，可选）。 */
@Data
public class RemarkRequest {

    @Size(max = 500, message = "原因过长")
    private String reason;
}
