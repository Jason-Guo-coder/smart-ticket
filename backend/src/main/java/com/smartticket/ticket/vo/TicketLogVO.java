package com.smartticket.ticket.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 工单时间线条目（含操作人姓名，B2）。 */
@Data
public class TicketLogVO {
    private String action;
    private Long operatorId;
    private String operatorName;
    private String remark;
    private LocalDateTime createTime;
}
