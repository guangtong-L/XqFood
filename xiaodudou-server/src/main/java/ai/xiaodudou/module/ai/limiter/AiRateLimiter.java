package ai.xiaodudou.module.ai.limiter;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.common.result.ResultCode;
import ai.xiaodudou.module.user.entity.User;
import ai.xiaodudou.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** AI 日额度与分钟窗口的 Redis 原子限流。生产 AI 关闭时也保持实现本身安全。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiRateLimiter {

    private final StringRedisTemplate redis;
    private final UserMapper userMapper;

    @Value("${xiaodudou.rate-limit.ai-free-daily:5}")
    private int freeDailyLimit;

    @Value("${xiaodudou.rate-limit.ai-vip-daily:50}")
    private int vipDailyLimit;

    @Value("${xiaodudou.rate-limit.ai-burst-per-min:3}")
    private int burstPerMin;

    private static final DateTimeFormatter MINUTE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    static final DefaultRedisScript<Long> CONSUME_SCRIPT = new DefaultRedisScript<>("""
            local burst = tonumber(redis.call('GET', KEYS[1]) or '0')
            local daily = tonumber(redis.call('GET', KEYS[2]) or '0')
            if burst >= tonumber(ARGV[1]) then return -1 end
            if daily >= tonumber(ARGV[2]) then return -2 end
            burst = redis.call('INCR', KEYS[1])
            if burst == 1 then redis.call('EXPIRE', KEYS[1], ARGV[3]) end
            daily = redis.call('INCR', KEYS[2])
            if daily == 1 then redis.call('EXPIRE', KEYS[2], ARGV[4]) end
            return daily
            """, Long.class);
    static final DefaultRedisScript<Long> ROLLBACK_SCRIPT = new DefaultRedisScript<>("""
            for _, key in ipairs(KEYS) do
              local current = tonumber(redis.call('GET', key) or '0')
              if current > 0 then redis.call('DECR', key) end
            end
            return 1
            """, Long.class);

    public QuotaLease checkAndConsume(Long userId) {
        boolean isVip = isVipUser(userId);
        int dailyLimit = isVip ? vipDailyLimit : freeDailyLimit;
        String slot = "{" + userId + "}";
        String minuteKey = "ai:quota:" + slot + ":burst:" + LocalDateTime.now().format(MINUTE_FMT);
        String dailyKey = "ai:quota:" + slot + ":daily:" + LocalDate.now();
        try {
            Long result = redis.execute(CONSUME_SCRIPT, List.of(minuteKey, dailyKey),
                    Integer.toString(burstPerMin), Integer.toString(dailyLimit), "70", "129600");
            if (result == null) throw new IllegalStateException("empty redis result");
            if (result == -1L) {
                log.warn("ai_rate_limit_exceeded dimension=burst userId={}", userId);
                throw new BusinessException(ResultCode.RATE_LIMIT, "操作过快，请稍后再试");
            }
            if (result == -2L) {
                log.info("ai_rate_limit_exceeded dimension=daily userId={} vip={}", userId, isVip);
                throw new BusinessException(ResultCode.AI_QUOTA_USED_UP, "今日 AI 额度已用完，请明日再试");
            }
            return new QuotaLease(minuteKey, dailyKey);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("ai_rate_limit_unavailable errorType={}", e.getClass().getSimpleName());
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "AI 服务暂时不可用，请稍后重试");
        }
    }

    public RemainingQuota getRemaining(Long userId) {
        boolean isVip = isVipUser(userId);
        int limit = isVip ? vipDailyLimit : freeDailyLimit;
        String dailyKey = "ai:quota:{" + userId + "}:daily:" + LocalDate.now();
        try {
            String val = redis.opsForValue().get(dailyKey);
            int used = val == null ? 0 : Math.max(0, Integer.parseInt(val));
            return new RemainingQuota(limit, used, Math.max(0, limit - used), isVip);
        } catch (Exception e) {
            log.error("ai_rate_limit_read_failed errorType={}", e.getClass().getSimpleName());
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "AI 服务暂时不可用，请稍后重试");
        }
    }

    /** 回滚创建时记录的精确窗口，跨分钟也不会误减别的计数；Lua 保证不会减成负数。 */
    public void rollback(QuotaLease lease) {
        if (lease == null) return;
        try {
            redis.execute(ROLLBACK_SCRIPT, List.of(lease.minuteKey(), lease.dailyKey()));
        } catch (Exception e) {
            log.warn("ai_rate_limit_rollback_failed errorType={}", e.getClass().getSimpleName());
        }
    }

    private boolean isVipUser(Long userId) {
        try {
            User user = userMapper.selectById(userId);
            return user != null && user.getVipLevel() != null && user.getVipLevel() > 0
                    && (user.getVipExpireAt() == null || user.getVipExpireAt().isAfter(LocalDateTime.now()));
        } catch (Exception e) {
            return false;
        }
    }

    public record RemainingQuota(int limit, int used, int remaining, boolean isVip) { }
    public record QuotaLease(String minuteKey, String dailyKey) { }
}
