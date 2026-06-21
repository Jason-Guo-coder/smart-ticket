package com.smartticket.common.exception;

import com.smartticket.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常。由全局异常处理统一转 Result（ARCHITECTURE §5.2 / B6）。
 * 业务代码抛此异常，不在 controller 里 try-catch 吞异常。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.FAIL.getCode();
    }

    public BizException(ResultCode rc) {
        super(rc.getMessage());
        this.code = rc.getCode();
    }

    public BizException(ResultCode rc, String message) {
        super(message);
        this.code = rc.getCode();
    }
}
