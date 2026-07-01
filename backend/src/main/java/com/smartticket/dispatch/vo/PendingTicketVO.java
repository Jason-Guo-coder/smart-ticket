package com.smartticket.dispatch.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 派单页待派单卡片：工单信息 + AI 建议工程师。 */
@Data
public class PendingTicketVO {

    private Long id;
    private String ticketNo;
    private String title;
    private String category;
    private String categoryLabel;
    private String priority;
    private String priorityLabel;
    private LocalDateTime createTime;
    private LocalDateTime slaDeadline;
    private Integer slaOverdue;

    /** AI 建议（按类别 + 负载推荐）工程师。 */
    private Long suggestedEngineerId;
    private String suggestedEngineerName;
}
