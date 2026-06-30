package com.smartticket.ticket.enums;

/** 工单优先级（DESIGN §2.3）。 */
public enum Priority {
    HIGH("高"),
    MID("中"),
    LOW("低");

    private final String label;

    Priority(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
