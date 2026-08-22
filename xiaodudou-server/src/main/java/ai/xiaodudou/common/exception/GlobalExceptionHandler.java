package ai.xiaodudou.common.exception;

import ai.xiaodudou.common.result.Result;
import ai.xiaodudou.common.result.ResultCode;
import cn.dev33.satoken.exception.NotLoginException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理
 *
 * @author xiaodudou
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        log.warn("business_exception code={}", e.getCode());
        return ResponseEntity.status(statusFor(e.getCode()))
                .body(Result.fail(e.getCode(), e.getMessage()));
    }

    /** 未登录 */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleNotLogin(NotLoginException e) {
        return Result.fail(ResultCode.UNAUTHORIZED);
    }

    /** 参数校验 - @Valid */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("参数校验失败");
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), msg);
    }

    /** 参数校验 - @Validated */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("参数校验失败");
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), msg);
    }

    /** 参数校验 - @PathVariable / @RequestParam */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraint(ConstraintViolationException e) {
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), e.getMessage());
    }

    /** JSON 语法、日期或枚举格式错误。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleUnreadable(HttpMessageNotReadableException e) {
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), "请求格式错误");
    }

    /** 容器层在进入控制器前拦截的超大 multipart。 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Result<Void> handleMaxUpload(MaxUploadSizeExceededException e) {
        return Result.fail(ResultCode.PAYLOAD_TOO_LARGE.getCode(), "图片不能超过 5MB");
    }

    /** 兜底 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleUnknown(Exception e) {
        // 不记录异常消息和请求内容，避免数据库/第三方异常把敏感字段带入日志；request-id 已在 MDC。
        log.error("unhandled_exception errorType={}", e.getClass().getSimpleName());
        return Result.fail(ResultCode.SERVER_ERROR);
    }

    private HttpStatus statusFor(Integer code) {
        if (code == null) return HttpStatus.INTERNAL_SERVER_ERROR;
        if (code.equals(ResultCode.BAD_REQUEST.getCode()) || code.equals(ResultCode.LOGIN_FAILED.getCode())) {
            return HttpStatus.BAD_REQUEST;
        }
        if (code.equals(ResultCode.UNAUTHORIZED.getCode())) return HttpStatus.UNAUTHORIZED;
        if (code.equals(ResultCode.FORBIDDEN.getCode())) return HttpStatus.FORBIDDEN;
        if (code.equals(ResultCode.NOT_FOUND.getCode())
                || code.equals(ResultCode.USER_NOT_FOUND.getCode())
                || code.equals(ResultCode.RECIPE_NOT_FOUND.getCode())
                || code.equals(ResultCode.ORDER_NOT_FOUND.getCode())) {
            return HttpStatus.NOT_FOUND;
        }
        if (code.equals(ResultCode.RATE_LIMIT.getCode()) || code.equals(ResultCode.AI_QUOTA_USED_UP.getCode())) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }
        if (code.equals(ResultCode.PAYLOAD_TOO_LARGE.getCode())) return HttpStatus.PAYLOAD_TOO_LARGE;
        if (code.equals(ResultCode.INVALID_IMAGE.getCode())) return HttpStatus.UNPROCESSABLE_ENTITY;
        if (code.equals(ResultCode.AI_INVALID_RESPONSE.getCode())) return HttpStatus.BAD_GATEWAY;
        if (code.equals(ResultCode.SERVICE_UNAVAILABLE.getCode())
                || code.equals(ResultCode.FEATURE_NOT_AVAILABLE.getCode())
                || code.equals(ResultCode.AI_TIMEOUT.getCode())
                || code.equals(ResultCode.AI_SERVICE_UNAVAILABLE.getCode())) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
