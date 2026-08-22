package ai.xiaodudou.module.user.security;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

/** 微信调用前执行的 Redis 原子固定窗口限流；Redis 异常时 fail-closed。 */
@Slf4j
@Component
public class LoginRateLimiter {

    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
            return count
            """, Long.class);

    private final StringRedisTemplate redis;
    private final ClientSourceResolver sourceResolver;
    private final int limit;
    private final long windowMillis;

    public LoginRateLimiter(StringRedisTemplate redis,
                            ClientSourceResolver sourceResolver,
                            @Value("${xiaodudou.rate-limit.login-attempts:10}") int limit,
                            @Value("${xiaodudou.rate-limit.login-window-seconds:60}") long windowSeconds) {
        if (limit < 1 || limit > 1000 || windowSeconds < 1 || windowSeconds > 3600) {
            throw new IllegalArgumentException("登录限流配置超出安全范围");
        }
        this.redis = redis;
        this.sourceResolver = sourceResolver;
        this.limit = limit;
        this.windowMillis = windowSeconds * 1000L;
    }

    public void check(HttpServletRequest request) {
        String key = "auth:login:source:" + digest(sourceResolver.resolve(request));
        try {
            Long count = redis.execute(SCRIPT, List.of(key), Long.toString(windowMillis));
            if (count == null) throw new IllegalStateException("empty redis result");
            if (count > limit) {
                log.warn("login_rate_limited");
                throw new BusinessException(ResultCode.RATE_LIMIT, "登录请求过于频繁，请稍后再试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("login_rate_limit_unavailable errorType={}", e.getClass().getSimpleName());
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "登录服务暂时不可用，请稍后重试");
        }
    }

    private static String digest(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
