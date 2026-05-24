package ai.xiaodudou.common.exception;

import ai.xiaodudou.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常 - 用于受控的预期异常
 *
 * @author xiaodudou
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String customMessage) {
        super(customMessage);
        this.code = resultCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
