package ai.xiaodudou.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 注销必须由用户显式输入完整确认语句。 */
public record DeleteAccountRequest(
        @NotBlank(message = "请输入注销确认语句")
        @Pattern(regexp = "^确认注销账号$", message = "请输入“确认注销账号”")
        String confirmation
) {
}
