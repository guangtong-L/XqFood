package ai.xiaodudou.module.action.controller;

import ai.xiaodudou.common.dto.PageResponse;
import ai.xiaodudou.common.result.Result;
import ai.xiaodudou.module.action.dto.CalendarResponse;
import ai.xiaodudou.module.action.dto.CheckinItemResponse;
import ai.xiaodudou.module.action.dto.CheckinRequest;
import ai.xiaodudou.module.action.dto.CheckinResponse;
import ai.xiaodudou.module.action.dto.DeleteCheckinResponse;
import ai.xiaodudou.module.action.dto.FavoriteResponse;
import ai.xiaodudou.module.action.service.UserActionService;
import ai.xiaodudou.module.recipe.dto.RecipeResponse;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@Tag(name = "05 - 收藏与打卡")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserActionController {
    private final UserActionService actionService;

    @PostMapping("/favorites/{recipeId}")
    @Operation(summary = "幂等收藏上架菜谱")
    public Result<FavoriteResponse> addFavorite(@PathVariable @Positive Long recipeId) {
        boolean created = actionService.addFavorite(StpUtil.getLoginIdAsLong(), recipeId);
        return Result.ok(new FavoriteResponse(created, !created));
    }

    @DeleteMapping("/favorites/{recipeId}")
    public Result<Void> removeFavorite(@PathVariable @Positive Long recipeId) {
        actionService.removeFavorite(StpUtil.getLoginIdAsLong(), recipeId);
        return Result.ok();
    }

    @GetMapping("/favorites/{recipeId}/check")
    public Result<Boolean> isFavorited(@PathVariable @Positive Long recipeId) {
        return Result.ok(actionService.isFavorited(StpUtil.getLoginIdAsLong(), recipeId));
    }

    @GetMapping("/favorites")
    @Operation(summary = "我的收藏分页列表")
    public Result<PageResponse<RecipeResponse>> favorites(
            @RequestParam(defaultValue = "1") @Min(1) @Max(10000) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) Integer size) {
        return Result.ok(actionService.favorites(StpUtil.getLoginIdAsLong(), page, size));
    }

    @PostMapping("/checkin")
    @Operation(summary = "明确餐次与份数后幂等记录已食用菜谱")
    public Result<CheckinResponse> checkin(@Valid @RequestBody CheckinRequest request) {
        return Result.ok(actionService.checkin(StpUtil.getLoginIdAsLong(), request));
    }

    @DeleteMapping("/checkin/{actionId}")
    @Operation(summary = "删除本人的打卡记录")
    public Result<DeleteCheckinResponse> deleteCheckin(@PathVariable @Positive Long actionId) {
        return Result.ok(new DeleteCheckinResponse(
                actionService.deleteCheckin(StpUtil.getLoginIdAsLong(), actionId)));
    }

    @GetMapping("/checkin/today")
    public Result<List<CheckinItemResponse>> today() {
        return Result.ok(actionService.today(StpUtil.getLoginIdAsLong()));
    }

    @GetMapping("/checkin/calendar")
    public Result<CalendarResponse> calendar(@RequestParam(required = false) String month) {
        return Result.ok(actionService.calendar(StpUtil.getLoginIdAsLong(), month));
    }
}
