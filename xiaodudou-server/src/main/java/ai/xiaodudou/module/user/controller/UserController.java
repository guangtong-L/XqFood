package ai.xiaodudou.module.user.controller;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.common.result.Result;
import ai.xiaodudou.common.result.ResultCode;
import ai.xiaodudou.module.user.entity.User;
import ai.xiaodudou.module.user.entity.UserProfile;
import ai.xiaodudou.module.user.mapper.UserMapper;
import ai.xiaodudou.module.user.mapper.UserProfileMapper;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户信息 + 阶段画像
 */
@Tag(name = "02 - 用户与画像")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;

    @GetMapping("/me")
    @Operation(summary = "当前登录用户信息")
    public Result<Map<String, Object>> me() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);

        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));

        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("profile", profile);
        return Result.ok(data);
    }

    @GetMapping("/profile")
    @Operation(summary = "查询当前用户的阶段画像")
    public Result<UserProfile> getProfile() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId)));
    }

    @PostMapping("/profile")
    @Operation(summary = "保存或更新阶段画像")
    public Result<UserProfile> saveProfile(@RequestBody UserProfile body) {
        Long userId = StpUtil.getLoginIdAsLong();
        body.setUserId(userId);

        UserProfile exist = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));

        if (exist == null) {
            userProfileMapper.insert(body);
        } else {
            body.setId(exist.getId());
            userProfileMapper.updateById(body);
        }
        return Result.ok(body);
    }
}
