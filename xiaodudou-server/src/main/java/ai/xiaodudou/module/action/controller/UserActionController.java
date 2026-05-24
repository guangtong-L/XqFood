package ai.xiaodudou.module.action.controller;

import ai.xiaodudou.common.result.Result;
import ai.xiaodudou.module.action.entity.UserRecipeAction;
import ai.xiaodudou.module.action.mapper.UserRecipeActionMapper;
import ai.xiaodudou.module.recipe.entity.Recipe;
import ai.xiaodudou.module.recipe.mapper.RecipeMapper;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Tag(name = "05 - 收藏与打卡")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserActionController {

    private final UserRecipeActionMapper actionMapper;
    private final RecipeMapper recipeMapper;

    // ============ 收藏 ============

    @PostMapping("/favorites/{recipeId}")
    @Operation(summary = "收藏菜谱")
    public Result<Void> addFavorite(@PathVariable Long recipeId) {
        Long userId = StpUtil.getLoginIdAsLong();
        // 幂等：已收藏直接返回
        Long count = actionMapper.selectCount(
                new LambdaQueryWrapper<UserRecipeAction>()
                        .eq(UserRecipeAction::getUserId, userId)
                        .eq(UserRecipeAction::getRecipeId, recipeId)
                        .eq(UserRecipeAction::getAction, UserRecipeAction.FAVORITE));
        if (count > 0) return Result.ok();

        UserRecipeAction a = new UserRecipeAction();
        a.setUserId(userId);
        a.setRecipeId(recipeId);
        a.setAction(UserRecipeAction.FAVORITE);
        a.setCreatedAt(LocalDateTime.now());  // 该表只有 created_at，没用 @TableField fill，手动赋值
        actionMapper.insert(a);
        return Result.ok();
    }

    @DeleteMapping("/favorites/{recipeId}")
    @Operation(summary = "取消收藏")
    public Result<Void> removeFavorite(@PathVariable Long recipeId) {
        Long userId = StpUtil.getLoginIdAsLong();
        actionMapper.delete(new LambdaQueryWrapper<UserRecipeAction>()
                .eq(UserRecipeAction::getUserId, userId)
                .eq(UserRecipeAction::getRecipeId, recipeId)
                .eq(UserRecipeAction::getAction, UserRecipeAction.FAVORITE));
        return Result.ok();
    }

    @GetMapping("/favorites/{recipeId}/check")
    @Operation(summary = "检查是否已收藏")
    public Result<Boolean> isFavorited(@PathVariable Long recipeId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long count = actionMapper.selectCount(new LambdaQueryWrapper<UserRecipeAction>()
                .eq(UserRecipeAction::getUserId, userId)
                .eq(UserRecipeAction::getRecipeId, recipeId)
                .eq(UserRecipeAction::getAction, UserRecipeAction.FAVORITE));
        return Result.ok(count > 0);
    }

    @GetMapping("/favorites")
    @Operation(summary = "我的收藏列表")
    public Result<List<Recipe>> myFavorites() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<UserRecipeAction> actions = actionMapper.selectList(
                new LambdaQueryWrapper<UserRecipeAction>()
                        .eq(UserRecipeAction::getUserId, userId)
                        .eq(UserRecipeAction::getAction, UserRecipeAction.FAVORITE)
                        .orderByDesc(UserRecipeAction::getCreatedAt));

        if (actions.isEmpty()) return Result.ok(List.of());

        List<Long> ids = actions.stream().map(UserRecipeAction::getRecipeId).toList();
        List<Recipe> recipes = recipeMapper.selectBatchIds(ids);

        // 保持收藏时间倒序
        Map<Long, Recipe> recipeMap = new HashMap<>();
        for (Recipe r : recipes) recipeMap.put(r.getId(), r);
        List<Recipe> sorted = new ArrayList<>();
        for (UserRecipeAction a : actions) {
            Recipe r = recipeMap.get(a.getRecipeId());
            if (r != null) sorted.add(r);
        }
        return Result.ok(sorted);
    }

    // ============ 打卡 ============

    @Data
    public static class CheckinReq {
        private Long recipeId;
        /** breakfast / lunch / dinner / snack 可选 */
        private String mealType;
    }

    @PostMapping("/checkin")
    @Operation(summary = "打卡（完成了某个菜谱）")
    public Result<Void> checkin(@RequestBody CheckinReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserRecipeAction a = new UserRecipeAction();
        a.setUserId(userId);
        a.setRecipeId(req.getRecipeId());
        a.setAction(UserRecipeAction.COOK);
        a.setCreatedAt(LocalDateTime.now());
        actionMapper.insert(a);
        return Result.ok();
    }

    @GetMapping("/checkin/today")
    @Operation(summary = "今日打卡列表")
    public Result<List<Map<String, Object>>> today() {
        Long userId = StpUtil.getLoginIdAsLong();
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        List<UserRecipeAction> actions = actionMapper.selectList(
                new LambdaQueryWrapper<UserRecipeAction>()
                        .eq(UserRecipeAction::getUserId, userId)
                        .eq(UserRecipeAction::getAction, UserRecipeAction.COOK)
                        .ge(UserRecipeAction::getCreatedAt, dayStart)
                        .lt(UserRecipeAction::getCreatedAt, dayEnd)
                        .orderByDesc(UserRecipeAction::getCreatedAt));

        if (actions.isEmpty()) return Result.ok(List.of());

        List<Long> recipeIds = actions.stream().map(UserRecipeAction::getRecipeId).toList();
        List<Recipe> recipes = recipeMapper.selectBatchIds(recipeIds);
        Map<Long, Recipe> map = new HashMap<>();
        for (Recipe r : recipes) map.put(r.getId(), r);

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserRecipeAction a : actions) {
            Recipe r = map.get(a.getRecipeId());
            if (r == null) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("actionId", a.getId());
            item.put("recipeId", r.getId());
            item.put("title", r.getTitle());
            item.put("coverUrl", r.getCoverUrl());
            item.put("nutrition", r.getNutrition());
            item.put("checkedAt", a.getCreatedAt());
            result.add(item);
        }
        return Result.ok(result);
    }

    @GetMapping("/checkin/calendar")
    @Operation(summary = "月度打卡日历，month 格式 yyyy-MM")
    public Result<Map<String, Object>> calendar(@RequestParam(required = false) String month) {
        Long userId = StpUtil.getLoginIdAsLong();
        YearMonth ym;
        try {
            ym = month == null || month.isBlank() ? YearMonth.now() : YearMonth.parse(month);
        } catch (Exception e) {
            ym = YearMonth.now();
        }

        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();

        List<Map<String, Object>> rows = actionMapper.aggregateCookByDay(userId, start, end);

        Map<String, Integer> dayCount = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object day = row.get("day");
            Object cnt = row.get("count");
            if (day != null && cnt != null) {
                dayCount.put(day.toString(), ((Number) cnt).intValue());
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("month", ym.toString());
        data.put("daysInMonth", ym.lengthOfMonth());
        data.put("checkinDays", dayCount.size());
        data.put("totalCheckins", dayCount.values().stream().mapToInt(Integer::intValue).sum());
        data.put("dayCount", dayCount);
        return Result.ok(data);
    }
}
