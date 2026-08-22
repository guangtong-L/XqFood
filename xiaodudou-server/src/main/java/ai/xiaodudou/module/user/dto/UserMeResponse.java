package ai.xiaodudou.module.user.dto;

import java.time.LocalDateTime;

/** 当前用户对外白名单，禁止直接序列化 User 数据库实体。 */
public record UserMeResponse(
        Long id,
        String nickname,
        String avatarUrl,
        Integer vipLevel,
        LocalDateTime vipExpireAt,
        LocalDateTime createdAt,
        ProfileResponse profile
) {
}
