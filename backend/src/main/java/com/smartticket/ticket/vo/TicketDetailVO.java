package com.smartticket.ticket.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 工单详情：工单主体 + 中文标签 + 相关人姓名 + 时间线 + 解决方案 + SLA 截止。 */
@Data
public class TicketDetailVO {

    private Long id;
    private String ticketNo;
    private String title;
    private String content;
    private String imageUrl;

    private String category;
    private String categoryLabel;
    private String priority;
    private String priorityLabel;
    private String status;
    private String statusLabel;

    private Long creatorId;
    private String creatorName;
    private Long assigneeId;
    private String assigneeName;

    private LocalDateTime slaDeadline;
    private Integer slaOverdue;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 评价（已评价工单，否则为空）。 */
    private Integer evalScore;
    private String evalTags;
    private String evalComment;
    private LocalDateTime evalTime;

    /** 时间线（按发生时间升序）。 */
    private List<TicketLogVO> timeline;
    /** 解决方案（按提交时间升序，可能多条）。 */
    private List<TicketSolutionVO> solutions;
}
