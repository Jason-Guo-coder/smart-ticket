package com.smartticket.common.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 统一分页返回体。配合 {@link Result} 使用：Result.ok(PageResult.of(total, list))。
 * 跨模块复用（工单/派单/审计/统计分页），仅承载 total + list，不含分页请求参数。
 */
@Data
public class PageResult<T> implements Serializable {

    private long total;
    private List<T> list;

    public PageResult() {
    }

    public PageResult(long total, List<T> list) {
        this.total = total;
        this.list = list;
    }

    public static <T> PageResult<T> of(long total, List<T> list) {
        return new PageResult<>(total, list);
    }
}
