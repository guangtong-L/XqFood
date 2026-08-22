package ai.xiaodudou.module.user.security;

import ai.xiaodudou.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoginRateLimiterTest {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void allowsWithinLimitAndRejectsExcessAtomically() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ClientSourceResolver resolver = mock(ClientSourceResolver.class);
        HttpServletRequest request = new MockHttpServletRequest();
        when(resolver.resolve(request)).thenReturn("203.0.113.10");
        when(redis.execute(any(RedisScript.class), anyList(), any())).thenReturn(3L, 4L);
        LoginRateLimiter limiter = new LoginRateLimiter(redis, resolver, 3, 60);

        limiter.check(request);
        assertThatThrownBy(() -> limiter.check(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("频繁");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void redisFailureIsFailClosed() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ClientSourceResolver resolver = mock(ClientSourceResolver.class);
        HttpServletRequest request = new MockHttpServletRequest();
        when(resolver.resolve(request)).thenReturn("203.0.113.10");
        when(redis.execute(any(RedisScript.class), anyList(), any()))
                .thenThrow(new RedisConnectionFailureException("down"));

        assertThatThrownBy(() -> new LoginRateLimiter(redis, resolver, 3, 60).check(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("暂时不可用");
    }
}
