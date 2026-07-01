package com.smartticket.evaluation.service;

import com.smartticket.evaluation.dto.EvaluateRequest;

/** 评价服务。评价落库 + 状态流转（DONE→RATED）同事务原子提交。 */
public interface EvaluationService {

    /** 报修人对已完成工单评价（仅本人/ADMIN，仅 DONE 可评，重复被拦截）。 */
    void evaluate(Long ticketId, Long userId, String role, EvaluateRequest req);
}
