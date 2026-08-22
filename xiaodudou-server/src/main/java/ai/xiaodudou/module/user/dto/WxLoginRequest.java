package ai.xiaodudou.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 微信登录输入白名单。 */
public record WxLoginRequest(
        @NotBlank(message = "code 不能为空")
        @Size(max = 256, message = "code 长度不能超过 256")
        String code,

        @Size(max = 64, message = "昵称长度不能超过 64")
        String nickname,

        @Size(max = 512, message = "头像地址长度不能超过 512")
        @Pattern(regexp = "^$|^https://[^\\s]+$", message = "头像地址必须是 HTTPS URL")
        String avatarUrl
) {
}
