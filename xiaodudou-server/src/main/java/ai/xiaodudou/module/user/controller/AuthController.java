package ai.xiaodudou.module.user.controller;

import ai.xiaodudou.common.result.Result;
import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.common.result.ResultCode;
import ai.xiaodudou.config.RuntimeModePolicy;
import ai.xiaodudou.module.user.client.WechatClient;
import ai.xiaodudou.module.user.client.WechatClient.Code2SessionResult;
import ai.xiaodudou.module.user.entity.User;
import ai.xiaodudou.module.user.dto.WxLoginRequest;
import ai.xiaodudou.module.user.dto.WxLoginResponse;
import ai.xiaodudou.module.user.security.LoginRateLimiter;
import ai.xiaodudou.module.user.service.AuthAccountService;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

/**
 * 鉴权接口
 * 真实模式调用微信 jscode2session；开发 Mock 仅限 dev/local 且必须显式开启。
 */
@Slf4j
@Tag(name = "01 - 鉴权")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthAccountService accountService;
    private final WechatClient wechatClient;
    private final StringRedisTemplate redis;
    private final RuntimeModePolicy runtimeModePolicy;
    private final LoginRateLimiter loginRateLimiter;

    @SaIgnore
    @PostMapping("/wx-login")
    @Operation(summary = "微信登录")
    public Result<WxLoginResponse> wxLogin(@Valid @RequestBody WxLoginRequest req,
                                           HttpServletRequest request) {
        // 必须在调用微信前完成；Redis 故障时 fail-closed，避免失去防刷边界。
        loginRateLimiter.check(request);
        String openid;
        String unionid = null;
        boolean realMode = wechatClient.isReady();

        if (realMode) {
            // ===== 真实模式：调微信 jscode2session =====
            Code2SessionResult wxResult = wechatClient.code2Session(req.code());
            openid = wxResult.getOpenid();
            unionid = wxResult.getUnionid();

            // session_key 服务端保留（用于解密用户信息/数据签名校验）
            // 永远不返回前端
            if (StrUtil.isNotBlank(wxResult.getSessionKey())) {
                try {
                    redis.opsForValue().set(
                            "wx:session:" + digest(openid),
                            wxResult.getSessionKey(),
                            Duration.ofDays(7)
                    );
                } catch (Exception e) {
                    log.error("wechat_session_store_failed errorType={}", e.getClass().getSimpleName());
                    throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "登录服务暂时不可用，请稍后重试");
                }
            }
            log.info("wechat_login_succeeded mode=real");
        } else {
            if (!runtimeModePolicy.isMockLoginAllowed()) {
                throw new BusinessException(ResultCode.LOGIN_FAILED,
                        "真实微信登录尚未配置，当前环境禁止 Mock 登录");
            }
            // ===== 开发 Mock 模式：必须 dev/local + 显式开关 =====
            String code = req.code();
            if (StrUtil.isBlank(code)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "code 不能为空");
            }
            openid = "mock_" + code;
            log.info("wechat_login_succeeded mode=mock");
        }

        User user = accountService.findOrCreate(openid, unionid, req, realMode);

        StpUtil.login(user.getId());

        return Result.ok(new WxLoginResponse(
                StpUtil.getTokenValue(), user.getId(), user.getNickname(), user.getAvatarUrl(),
                user.getVipLevel(), realMode ? "real" : "mock"));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.ok();
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
