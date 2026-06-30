package com.smartticket.ticket.vo;

import com.smartticket.ai.model.AiSuggestion;
import lombok.Data;

/** 提交工单出参：工单号 + AI 建议。 */
@Data
public class SubmitResultVO {
    private Long ticketId;
    private String ticketNo;
    private AiSuggestion ai;
}
