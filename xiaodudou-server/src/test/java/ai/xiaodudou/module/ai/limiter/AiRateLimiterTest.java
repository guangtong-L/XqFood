package ai.xiaodudou.module.ai.limiter;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.module.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiRateLimiterTest {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void consumeUsesOneAtomicScriptAndReturnsExactRollbackKeys() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), any(), any(), any(), any())).thenReturn(1L);
        AiRateLimiter limiter = limiter(redis);

        AiRateLimiter.QuotaLease lease = limiter.checkAndConsume(7L);

        assertThat(lease.minuteKey()).contains("{7}", ":burst:");
        assertThat(lease.dailyKey()).contains("{7}", ":daily:");
        limiter.rollback(lease);
        verify(redis).execute(AiRateLimiter.ROLLBACK_SCRIPT,
                java.util.List.of(lease.minuteKey(), lease.dailyKey()));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void limitsAndRedisFailuresAreExplicit() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), any(), any(), any(), any()))
                .thenReturn(-1L)
                .thenThrow(new RedisConnectionFailureException("down"));
        AiRateLimiter limiter = limiter(redis);

        assertThatThrownBy(() -> limiter.checkAndConsume(7L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("操作过快");
        assertThatThrownBy(() -> limiter.checkAndConsume(7L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("暂时不可用");
    }

    @SuppressWarnings("unchecked")
    @Test
    void negativeStoredCountNeverProducesNegativeUsage() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(any())).thenReturn("-4");

        AiRateLimiter.RemainingQuota remaining = limiter(redis).getRemaining(7L);

        assertThat(remaining.used()).isZero();
        assertThat(remaining.remaining()).isEqualTo(5);
        assertThat(AiRateLimiter.ROLLBACK_SCRIPT.getScriptAsString()).contains("current > 0");
    }

    private static AiRateLimiter limiter(StringRedisTemplate redis) {
        AiRateLimiter limiter = new AiRateLimiter(redis, mock(UserMapper.class));
        ReflectionTestUtils.setField(limiter, "freeDailyLimit", 5);
        ReflectionTestUtils.setField(limiter, "vipDailyLimit", 50);
        ReflectionTestUtils.setField(limiter, "burstPerMin", 3);
        return limiter;
    }
}
