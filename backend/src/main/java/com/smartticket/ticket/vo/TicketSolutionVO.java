package com.smartticket.ticket.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 解决方案条目（含工程师姓名）。 */
@Data
public class TicketSolutionVO {
    private Long engineerId;
    private String engineerName;
    private String solutionText;
    private String imageUrl;
    private LocalDateTime createTime;
}
