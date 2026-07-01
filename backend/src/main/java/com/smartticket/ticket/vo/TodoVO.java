package com.smartticket.ticket.vo;

import lombok.Data;

import java.util.List;

/** 工程师待办页数据：顶部统计条 + 待办卡片列表。 */
@Data
public class TodoVO {

    private Stats stats;
    private List<TicketListItemVO> list;

    /** 统计条：待接单 / 我处理中 / 我今日完成 / 我平均处理时长(小时)。 */
    @Data
    public static class Stats {
        private long pending;
        private long processing;
        private long doneToday;
        private double avgHours;
    }
}
