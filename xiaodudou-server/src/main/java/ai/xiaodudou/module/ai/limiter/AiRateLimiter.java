package ai.xiaodudou.module.ai.limiter;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.common.result.ResultCode;
import ai.xiaodudou.module.user.entity.User;
import ai.xiaodudou.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AI 接口限流：日额度 + 分钟级防刷
 * - 日额度：免费 5 / VIP 50（识别+推荐合并计数）
 * - 分钟级：每用户每分钟最多 3 次（防刷点击）
 *
 * 超限抛 BusinessException，前端按 code 区分提示
 */
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

    /**
     * 检查并扣减额度。
     * 顺序：先分钟级（防刷）→ 再日额度（防滥用）
     * 任一超限直接抛异常，不消耗对方额度
     */
    public void checkAndConsume(Long userId) {
        // ===== 1. 分钟级 =====
        String minuteKey = "ai:burst:" + userId + ":" + LocalDateTime.now().format(MINUTE_FMT);
        Long burstCount = redis.opsForValue().increment(minuteKey);
        if (burstCount != null && burstCount == 1L) {
            redis.expire(minuteKey, Duration.ofSeconds(70));
        }
        if (burstCount != null && burstCount > burstPerMin) {
            log.warn("[RateLimit] 用户 {} 分钟级超限 {}/{}", userId, burstCount, burstPerMin);
            throw new BusinessException(ResultCode.RATE_LIMIT,
                    "操作过快，请稍后再试");
        }

        // ===== 2. 日额度 =====
        boolean isVip = isVipUser(userId);
        int limit = isVip ? vipDailyLimit : freeDailyLimit;
        String dailyKey = "ai:daily:" + userId + ":" + LocalDate.now();
        Long dailyCount = redis.opsForValue().increment(dailyKey);
        if (dailyCount != null && dailyCount == 1L) {
            redis.expire(dailyKey, Duration.ofHours(36));  // 跨日宽容
        }
        if (dailyCount != null && dailyCount > limit) {
            log.info("[RateLimit] 用户 {} 日额度用尽 {}/{} isVip={}", userId, dailyCount, limit, isVip);
            String msg = isVip
                    ? "今日 AI 额度已用完（" + limit + " 次/日）"
                    : "今日免费额度已用完（" + freeDailyLimit + " 次/日），开通月子卡解锁 " + vipDailyLimit + " 次/日";
            throw new BusinessException(ResultCode.AI_QUOTA_USED_UP, msg);
        }
    }

    /** 查询用户剩余额度（前端展示用） */
    public RemainingQuota getRemaining(Long userId) {
        boolean isVip = isVipUser(userId);
        int limit = isVip ? vipDailyLimit : freeDailyLimit;
        String dailyKey = "ai:daily:" + userId + ":" + LocalDate.now();
        String val = redis.opsForValue().get(dailyKey);
        int used = val == null ? 0 : Integer.parseInt(val);
        return new RemainingQuota(limit, used, Math.max(0, limit - used), isVip);
    }

    /** 出错时回滚一次（如智谱调用失败，不应扣额度） */
    public void rollback(Long userId) {
        try {
            redis.opsForValue().decrement("ai:daily:" + userId + ":" + LocalDate.now());
            redis.opsForValue().decrement("ai:burst:" + userId + ":"
                    + LocalDateTime.now().format(MINUTE_FMT));
        } catch (Exception e) {
            log.warn("[RateLimit] 回滚失败 userId={}: {}", userId, e.getMessage());
        }
    }

    private boolean isVipUser(Long userId) {
        try {
            User u = userMapper.selectById(userId);
            return u != null && u.getVipLevel() != null && u.getVipLevel() > 0
                    && (u.getVipExpireAt() == null || u.getVipExpireAt().isAfter(LocalDateTime.now()));
        } catch (Exception e) {
            return false;
        }
    }

    public record RemainingQuota(int limit, int used, int remaining, boolean isVip) {}
}
