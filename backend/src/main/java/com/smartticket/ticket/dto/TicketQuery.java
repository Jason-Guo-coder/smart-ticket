package com.smartticket.ticket.dto;

import lombok.Data;

/** 我的工单分页查询入参（状态/关键词，仅本人）。 */
@Data
public class TicketQuery {

    /** 页码，从 1 开始。 */
    private Integer pageNum = 1;
    /** 每页条数（服务层裁剪上限，防超大分页）。 */
    private Integer pageSize = 10;

    /** 状态过滤（TicketStatus 枚举名，可空）。 */
    private String status;
    /** 关键词，匹配标题或工单号（可空）。 */
    private String keyword;
}
