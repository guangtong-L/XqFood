package ai.xiaodudou.module.user.controller;

import ai.xiaodudou.common.result.Result;
import ai.xiaodudou.module.user.entity.User;
import ai.xiaodudou.module.user.mapper.UserMapper;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 鉴权接口
 * M1 阶段提供 Mock 登录，等微信 AppID 下来后替换成 jscode2session
 */
@Slf4j
@Tag(name = "01 - 鉴权")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;

    @Data
    public static class WxLoginReq {
        /** 微信 wx.login() 返回的 code，M1 mock 阶段可任意字符串 */
        private String code;
        private String nickname;
        private String avatarUrl;
    }

    @SaIgnore
    @PostMapping("/wx-login")
    @Operation(summary = "微信登录（M1 Mock 版）")
    public Result<Map<String, Object>> wxLogin(@RequestBody WxLoginReq req) {
        // ⚠️ M1 Mock：用 code 作为 openid，等接入微信后改为 jscode2session
        String mockOpenid = "mock_" + (req.getCode() != null ? req.getCode() : IdUtil.simpleUUID().substring(0, 16));

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getWxOpenid, mockOpenid));

        if (user == null) {
            user = new User();
            user.setWxOpenid(mockOpenid);
            user.setNickname(req.getNickname() != null ? req.getNickname() : "小肚兜用户");
            user.setAvatarUrl(req.getAvatarUrl());
            user.setStatus(1);
            user.setVipLevel(0);
            userMapper.insert(user);
            log.info("新用户注册 openid={} id={}", mockOpenid, user.getId());
        } else {
            // 更新昵称/头像
            if (req.getNickname() != null) user.setNickname(req.getNickname());
            if (req.getAvatarUrl() != null) user.setAvatarUrl(req.getAvatarUrl());
            userMapper.updateById(user);
        }

        StpUtil.login(user.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("token", StpUtil.getTokenValue());
        data.put("userId", user.getId());
        data.put("nickname", user.getNickname());
        data.put("avatarUrl", user.getAvatarUrl());
        data.put("vipLevel", user.getVipLevel());
        return Result.ok(data);
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.ok();
    }
}
