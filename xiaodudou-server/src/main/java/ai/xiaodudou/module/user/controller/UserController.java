package ai.xiaodudou.module.user.controller;

import ai.xiaodudou.common.result.Result;
import ai.xiaodudou.module.user.dto.DeleteAccountRequest;
import ai.xiaodudou.module.user.dto.DeleteAccountResponse;
import ai.xiaodudou.module.user.dto.ProfileResponse;
import ai.xiaodudou.module.user.dto.SaveProfileRequest;
import ai.xiaodudou.module.user.dto.UserMeResponse;
import ai.xiaodudou.module.user.entity.User;
import ai.xiaodudou.module.user.service.AccountDeletionService;
import ai.xiaodudou.module.user.service.AccountStatusService;
import ai.xiaodudou.module.user.service.ProfileService;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 用户信息与加密画像接口。数据库实体永不作为请求或响应类型。 */
@Tag(name = "02 - 用户与画像")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final AccountStatusService accountStatusService;
    private final ProfileService profileService;
    private final AccountDeletionService deletionService;

    @GetMapping("/me")
    @Operation(summary = "当前登录用户信息")
    public Result<UserMeResponse> me() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = accountStatusService.requireActive(userId);
        return Result.ok(new UserMeResponse(
                user.getId(), user.getNickname(), user.getAvatarUrl(), user.getVipLevel(),
                user.getVipExpireAt(), user.getCreatedAt(), profileService.getResponse(userId)));
    }

    @GetMapping("/profile")
    @Operation(summary = "查询当前用户的阶段画像")
    public Result<ProfileResponse> getProfile() {
        return Result.ok(profileService.getResponse(StpUtil.getLoginIdAsLong()));
    }

    @PostMapping("/profile")
    @Operation(summary = "保存或更新加密阶段画像")
    public Result<ProfileResponse> saveProfile(@Valid @RequestBody SaveProfileRequest request) {
        return Result.ok(profileService.save(StpUtil.getLoginIdAsLong(), request));
    }

    @DeleteMapping("/me")
    @Operation(summary = "永久注销当前账号")
    public Result<DeleteAccountResponse> deleteMe(@Valid @RequestBody DeleteAccountRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(new DeleteAccountResponse(deletionService.deleteAccount(userId)));
    }
}
