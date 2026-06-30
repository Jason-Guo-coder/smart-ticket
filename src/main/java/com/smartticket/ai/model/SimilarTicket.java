package com.smartticket.ai.model;

import lombok.Data;

import java.io.Serializable;

/** 相似历史工单（AI 建议的一部分）。 */
@Data
public class SimilarTicket implements Serializable {
    private String ticketNo;
    private String title;
    private String solution;
}
