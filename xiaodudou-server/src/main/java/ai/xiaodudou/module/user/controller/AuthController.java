package ai.xiaodudou.module.user.controller;

import ai.xiaodudou.common.result.Result;
import ai.xiaodudou.module.user.client.WechatClient;
import ai.xiaodudou.module.user.client.WechatClient.Code2SessionResult;
import ai.xiaodudou.module.user.entity.User;
import ai.xiaodudou.module.user.mapper.UserMapper;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 鉴权接口
 * 双模式：
 *   - 真实模式：WechatClient.isReady() = true 时，调 jscode2session
 *   - Mock 模式：未配 AppID 或 enabled=false 时，用 code 作 fake openid
 */
@Slf4j
@Tag(name = "01 - 鉴权")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;
    private final WechatClient wechatClient;
    private final StringRedisTemplate redis;

    @Data
    public static class WxLoginReq {
        /** wx.login() 返回的 code（真实模式必填；Mock 模式可任意）*/
        private String code;
        /** 用户授权的昵称（chooseNickName）*/
        private String nickname;
        /** 用户授权的头像（chooseAvatar）*/
        private String avatarUrl;
    }

    @SaIgnore
    @PostMapping("/wx-login")
    @Operation(summary = "微信登录（双模式：真实 / Mock）")
    public Result<Map<String, Object>> wxLogin(@RequestBody WxLoginReq req) {
        String openid;
        String unionid = null;
        boolean realMode = wechatClient.isReady();

        if (realMode) {
            // ===== 真实模式：调微信 jscode2session =====
            Code2SessionResult wxResult = wechatClient.code2Session(req.getCode());
            openid = wxResult.getOpenid();
            unionid = wxResult.getUnionid();

            // session_key 服务端保留（用于解密用户信息/数据签名校验）
            // 永远不返回前端
            if (StrUtil.isNotBlank(wxResult.getSessionKey())) {
                redis.opsForValue().set(
                        "wx:session:" + openid,
                        wxResult.getSessionKey(),
                        Duration.ofDays(7)
                );
            }
            log.info("[Auth] 真实微信登录成功 openid={}", openid);
        } else {
            // ===== Mock 模式：用 code 作 fake openid（M1 演示）=====
            String code = req.getCode();
            if (StrUtil.isBlank(code)) code = IdUtil.simpleUUID().substring(0, 16);
            openid = "mock_" + code;
            log.info("[Auth] Mock 登录 openid={}（请在 application-local.yml 配置真实 wechat 启用真实模式）", openid);
        }

        // ===== 查/建用户 =====
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getWxOpenid, openid));

        if (user == null) {
            user = new User();
            user.setWxOpenid(openid);
            user.setWxUnionid(unionid);
            user.setNickname(StrUtil.isNotBlank(req.getNickname()) ? req.getNickname() : "小肚兜用户");
            user.setAvatarUrl(req.getAvatarUrl());
            user.setStatus(1);
            user.setVipLevel(0);
            userMapper.insert(user);
            log.info("[Auth] 新用户注册 id={} mode={}", user.getId(), realMode ? "real" : "mock");
        } else {
            boolean changed = false;
            if (StrUtil.isNotBlank(req.getNickname()) && !req.getNickname().equals(user.getNickname())) {
                user.setNickname(req.getNickname());
                changed = true;
            }
            if (StrUtil.isNotBlank(req.getAvatarUrl()) && !req.getAvatarUrl().equals(user.getAvatarUrl())) {
                user.setAvatarUrl(req.getAvatarUrl());
                changed = true;
            }
            if (StrUtil.isNotBlank(unionid) && !unionid.equals(user.getWxUnionid())) {
                user.setWxUnionid(unionid);
                changed = true;
            }
            if (changed) userMapper.updateById(user);
        }

        StpUtil.login(user.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("token", StpUtil.getTokenValue());
        data.put("userId", user.getId());
        data.put("nickname", user.getNickname());
        data.put("avatarUrl", user.getAvatarUrl());
        data.put("vipLevel", user.getVipLevel());
        data.put("loginMode", realMode ? "real" : "mock");
        return Result.ok(data);
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.ok();
    }
}
