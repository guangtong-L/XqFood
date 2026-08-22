package ai.xiaodudou.common.exception;

import ai.xiaodudou.common.result.ResultCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerHttpStatusTest {

    @Test
    void containerLevelOversizedUploadKeeps413BusinessCode() {
        assertThat(new GlobalExceptionHandler().handleMaxUpload(new MaxUploadSizeExceededException(1)).getCode())
                .isEqualTo(ResultCode.PAYLOAD_TOO_LARGE.getCode());
    }

    @ParameterizedTest
    @MethodSource("statusCases")
    void businessErrorsUseCorrectHttpStatus(ResultCode code, HttpStatus expected) {
        assertThat(new GlobalExceptionHandler().handleBusiness(new BusinessException(code)).getStatusCode())
                .isEqualTo(expected);
    }

    static Stream<Arguments> statusCases() {
        return Stream.of(
                Arguments.of(ResultCode.BAD_REQUEST, HttpStatus.BAD_REQUEST),
                Arguments.of(ResultCode.FORBIDDEN, HttpStatus.FORBIDDEN),
                Arguments.of(ResultCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND),
                Arguments.of(ResultCode.RATE_LIMIT, HttpStatus.TOO_MANY_REQUESTS),
                Arguments.of(ResultCode.SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE));
    }
}
