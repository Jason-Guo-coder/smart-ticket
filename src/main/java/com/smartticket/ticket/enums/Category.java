package com.smartticket.ticket.enums;

/** 工单类别（AI 自动判断，PRD §4.2）。 */
public enum Category {
    NETWORK("网络"),
    HARDWARE("硬件"),
    ACCOUNT("账号"),
    SOFTWARE("软件"),
    LOGISTICS("后勤");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
