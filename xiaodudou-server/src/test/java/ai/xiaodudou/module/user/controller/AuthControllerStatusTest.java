package ai.xiaodudou.module.user.controller;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.config.RuntimeModePolicy;
import ai.xiaodudou.module.user.client.WechatClient;
import ai.xiaodudou.module.user.dto.WxLoginRequest;
import ai.xiaodudou.module.user.entity.User;
import ai.xiaodudou.module.user.security.LoginRateLimiter;
import ai.xiaodudou.module.user.service.AuthAccountService;
import cn.dev33.satoken.stp.StpUtil;
import org.mockito.MockedStatic;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

class AuthControllerStatusTest {

    @Test
    void rateLimitRunsBeforeWechatModeOrExternalCall() {
        AuthAccountService accountService = mock(AuthAccountService.class);
        WechatClient wechat = mock(WechatClient.class);
        RuntimeModePolicy policy = mock(RuntimeModePolicy.class);
        LoginRateLimiter limiter = mock(LoginRateLimiter.class);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        when(wechat.isReady()).thenReturn(false);
        when(policy.isMockLoginAllowed()).thenReturn(true);
        User user = new User();
        user.setId(1L);
        user.setStatus(1);
        user.setDeleted(0);
        when(accountService.findOrCreate("mock_code", null,
                new WxLoginRequest("code", null, null), false)).thenReturn(user);

        AuthController controller = new AuthController(accountService, wechat,
                mock(StringRedisTemplate.class), policy, limiter);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getTokenValue).thenReturn("token");
            controller.wxLogin(new WxLoginRequest("code", null, null), servletRequest);
        }

        var order = inOrder(limiter, wechat);
        order.verify(limiter).check(servletRequest);
        order.verify(wechat).isReady();
    }

    @Test
    void disabledExistingAccountCannotLogInAgain() {
        AuthAccountService accountService = mock(AuthAccountService.class);
        WechatClient wechat = mock(WechatClient.class);
        RuntimeModePolicy policy = mock(RuntimeModePolicy.class);
        when(wechat.isReady()).thenReturn(false);
        when(policy.isMockLoginAllowed()).thenReturn(true);
        WxLoginRequest request = new WxLoginRequest("code-1", "昵称", "");
        doThrow(new BusinessException(ai.xiaodudou.common.result.ResultCode.FORBIDDEN, "账号已停用或注销"))
                .when(accountService).findOrCreate("mock_code-1", null, request, false);

        AuthController controller = new AuthController(accountService, wechat, mock(StringRedisTemplate.class),
                policy, mock(LoginRateLimiter.class));

        assertThatThrownBy(() -> controller.wxLogin(request, new MockHttpServletRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("停用或注销");
    }

    @Test
    void originalOpenidCanCreateFreshAccountAfterDeletionAnonymizedIt() {
        AuthAccountService accountService = mock(AuthAccountService.class);
        WechatClient wechat = mock(WechatClient.class);
        RuntimeModePolicy policy = mock(RuntimeModePolicy.class);
        when(wechat.isReady()).thenReturn(false);
        when(policy.isMockLoginAllowed()).thenReturn(true);
        User inserted = new User();
        inserted.setId(99L);
        inserted.setNickname("新账号");
        inserted.setStatus(1);
        inserted.setDeleted(0);
        WxLoginRequest request = new WxLoginRequest("original-code", "新账号", "");
        when(accountService.findOrCreate("mock_original-code", null, request, false)).thenReturn(inserted);

        AuthController controller = new AuthController(accountService, wechat, mock(StringRedisTemplate.class),
                policy, mock(LoginRateLimiter.class));
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getTokenValue).thenReturn("test-token");
            controller.wxLogin(request, new MockHttpServletRequest());
            stp.verify(() -> StpUtil.login(99L));
        }
        assertThat(inserted.getStatus()).isEqualTo(1);
    }
}
