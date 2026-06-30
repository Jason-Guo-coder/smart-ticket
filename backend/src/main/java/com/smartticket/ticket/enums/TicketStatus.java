package com.smartticket.ticket.enums;

import java.util.Set;

/**
 * 工单状态机（PRD §4.1 / B1）。
 * 合法流转：PENDING→PROCESSING→ACCEPTING→DONE→RATED；
 * 合法回退：转派 PROCESSING→PENDING、验收驳回 ACCEPTING→PROCESSING。
 * canTransfer 集中定义合法迁移，非法流转一律拒绝。
 */
public enum TicketStatus {

    PENDING("待派单"),
    PROCESSING("处理中"),
    ACCEPTING("待验收"),
    DONE("已完成"),
    RATED("已评价");

    private final String label;

    TicketStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 当前状态允许迁移到的目标状态集合。 */
    public Set<TicketStatus> nextStates() {
        return switch (this) {
            case PENDING -> Set.of(PROCESSING);
            case PROCESSING -> Set.of(ACCEPTING, PENDING); // 提交完成 / 转派
            case ACCEPTING -> Set.of(DONE, PROCESSING);    // 验收通过 / 驳回
            case DONE -> Set.of(RATED);
            case RATED -> Set.of();
        };
    }

    public boolean canTransferTo(TicketStatus target) {
        return nextStates().contains(target);
    }
}
