package ai.xiaodudou.module.user.dto;

/** 登录响应白名单。 */
public record WxLoginResponse(
        String token,
        Long userId,
        String nickname,
        String avatarUrl,
        Integer vipLevel,
        String loginMode
) {
}
