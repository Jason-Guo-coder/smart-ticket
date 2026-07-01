package com.smartticket.ticket.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 我的工单列表项（附中文标签，供前端徽章直接渲染）。 */
@Data
public class TicketListItemVO {

    private Long id;
    private String ticketNo;
    private String title;

    private String category;
    private String categoryLabel;
    private String priority;
    private String priorityLabel;
    private String status;
    private String statusLabel;

    private LocalDateTime createTime;
    private LocalDateTime slaDeadline;
    private Integer slaOverdue;
}
