package com.smartticket.dispatch.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 一键智能分派结果统计。 */
@Data
@AllArgsConstructor
public class AutoAllResultVO {
    private int total;
    private int success;
    private int failed;
}
