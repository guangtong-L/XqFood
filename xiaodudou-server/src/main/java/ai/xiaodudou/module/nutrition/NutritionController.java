package ai.xiaodudou.module.nutrition;

import ai.xiaodudou.common.result.Result;
import ai.xiaodudou.module.action.entity.UserRecipeAction;
import ai.xiaodudou.module.action.mapper.UserRecipeActionMapper;
import ai.xiaodudou.module.recipe.entity.Recipe;
import ai.xiaodudou.module.recipe.mapper.RecipeMapper;
import ai.xiaodudou.module.user.entity.UserProfile;
import ai.xiaodudou.module.user.mapper.UserProfileMapper;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 营养雷达：今日已摄入 vs 阶段目标 vs 百分比
 */
@Tag(name = "06 - 营养")
@RestController
@RequestMapping("/api/v1/nutrition")
@RequiredArgsConstructor
public class NutritionController {

    private final UserRecipeActionMapper actionMapper;
    private final RecipeMapper recipeMapper;
    private final UserProfileMapper userProfileMapper;

    private static final String[] DIMS = {"calories", "protein", "calcium", "iron", "vitA", "vitC"};

    @GetMapping("/today")
    @Operation(summary = "今日营养摄入 + 阶段目标 + 百分比")
    public Result<Map<String, Object>> today() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
        String stage = profile == null ? null : profile.getStageType();

        // 1. 今日打卡的菜谱列表
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        List<UserRecipeAction> actions = actionMapper.selectList(
                new LambdaQueryWrapper<UserRecipeAction>()
                        .eq(UserRecipeAction::getUserId, userId)
                        .eq(UserRecipeAction::getAction, UserRecipeAction.COOK)
                        .ge(UserRecipeAction::getCreatedAt, dayStart)
                        .lt(UserRecipeAction::getCreatedAt, dayEnd));

        // 2. 累加每道菜的营养
        Map<String, Double> actual = new LinkedHashMap<>();
        for (String d : DIMS) actual.put(d, 0.0);

        if (!actions.isEmpty()) {
            List<Long> recipeIds = actions.stream().map(UserRecipeAction::getRecipeId).distinct().toList();
            List<Recipe> recipes = recipeMapper.selectBatchIds(recipeIds);
            Map<Long, Recipe> recipeMap = new HashMap<>();
            for (Recipe r : recipes) recipeMap.put(r.getId(), r);

            for (UserRecipeAction a : actions) {
                Recipe r = recipeMap.get(a.getRecipeId());
                if (r == null || r.getNutrition() == null) continue;
                for (String d : DIMS) {
                    Object v = r.getNutrition().get(d);
                    if (v instanceof Number num) {
                        actual.merge(d, num.doubleValue(), Double::sum);
                    }
                }
            }
        }

        // 3. 阶段目标
        Map<String, Number> target = NutritionTarget.forStage(stage);

        // 4. 百分比（capped at 200%）
        Map<String, Integer> percent = new LinkedHashMap<>();
        for (String d : DIMS) {
            double a = actual.get(d);
            double t = target.get(d).doubleValue();
            int p = t > 0 ? (int) Math.round(a / t * 100) : 0;
            percent.put(d, Math.min(200, p));
        }

        // 5. 组装
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("stageType", stage);
        data.put("stageLabel", NutritionTarget.stageLabel(stage));
        data.put("date", LocalDate.now().toString());
        data.put("checkinCount", actions.size());
        data.put("actual", roundActual(actual));
        data.put("target", target);
        data.put("percent", percent);
        data.put("items", buildItems(actual, target, percent));
        return Result.ok(data);
    }

    /** 雷达图友好结构（前端直接绑） */
    private List<Map<String, Object>> buildItems(Map<String, Double> actual,
                                                  Map<String, Number> target,
                                                  Map<String, Integer> percent) {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, String> LABEL = Map.of(
                "calories", "热量",
                "protein", "蛋白质",
                "calcium", "钙",
                "iron", "铁",
                "vitA", "维A",
                "vitC", "维C"
        );
        Map<String, String> UNIT = Map.of(
                "calories", "kcal",
                "protein", "g",
                "calcium", "mg",
                "iron", "mg",
                "vitA", "μg",
                "vitC", "mg"
        );
        for (String d : DIMS) {
            Map<String, Object> it = new LinkedHashMap<>();
            it.put("key", d);
            it.put("name", LABEL.get(d));
            it.put("unit", UNIT.get(d));
            it.put("actual", Math.round(actual.get(d) * 10) / 10.0);
            it.put("target", target.get(d));
            it.put("percent", percent.get(d));
            list.add(it);
        }
        return list;
    }

    private Map<String, Double> roundActual(Map<String, Double> actual) {
        Map<String, Double> out = new LinkedHashMap<>();
        for (var e : actual.entrySet()) {
            out.put(e.getKey(), Math.round(e.getValue() * 10) / 10.0);
        }
        return out;
    }

    // ============ 周/月营养报告（雷达图详细页用）============
    @GetMapping("/report")
    @Operation(summary = "N 日营养报告（日均摄入 + 目标对比 + 热门菜谱）")
    public Result<Map<String, Object>> report(@RequestParam(defaultValue = "7") Integer days) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (days == null || days < 1) days = 1;
        if (days > 90) days = 90;

        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
        String stage = profile == null ? null : profile.getStageType();

        // 时间窗（含今天，向前 N-1 天）
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1L);
        LocalDateTime windowStart = startDate.atStartOfDay();
        LocalDateTime windowEnd = today.plusDays(1).atStartOfDay();

        List<UserRecipeAction> actions = actionMapper.selectList(
                new LambdaQueryWrapper<UserRecipeAction>()
                        .eq(UserRecipeAction::getUserId, userId)
                        .eq(UserRecipeAction::getAction, UserRecipeAction.COOK)
                        .ge(UserRecipeAction::getCreatedAt, windowStart)
                        .lt(UserRecipeAction::getCreatedAt, windowEnd));

        // 累加 N 日总摄入
        Map<String, Double> totalActual = new LinkedHashMap<>();
        for (String d : DIMS) totalActual.put(d, 0.0);

        // 统计热门菜谱 + 实际有打卡的天数
        Map<Long, Integer> recipeCount = new HashMap<>();
        Set<LocalDate> checkinDates = new HashSet<>();

        if (!actions.isEmpty()) {
            List<Long> recipeIds = actions.stream().map(UserRecipeAction::getRecipeId).distinct().toList();
            List<Recipe> recipes = recipeMapper.selectBatchIds(recipeIds);
            Map<Long, Recipe> recipeMap = new HashMap<>();
            for (Recipe r : recipes) recipeMap.put(r.getId(), r);

            for (UserRecipeAction a : actions) {
                checkinDates.add(a.getCreatedAt().toLocalDate());
                recipeCount.merge(a.getRecipeId(), 1, Integer::sum);
                Recipe r = recipeMap.get(a.getRecipeId());
                if (r == null || r.getNutrition() == null) continue;
                for (String d : DIMS) {
                    Object v = r.getNutrition().get(d);
                    if (v instanceof Number num) {
                        totalActual.merge(d, num.doubleValue(), Double::sum);
                    }
                }
            }
        }

        // 日均（按 days 平均，不按 checkinDays，避免"只打卡 1 天却算 100% 达标"）
        Map<String, Double> avgActual = new LinkedHashMap<>();
        for (String d : DIMS) avgActual.put(d, totalActual.get(d) / days);

        Map<String, Number> target = NutritionTarget.forStage(stage);

        Map<String, Integer> avgPercent = new LinkedHashMap<>();
        for (String d : DIMS) {
            double a = avgActual.get(d);
            double t = target.get(d).doubleValue();
            int p = t > 0 ? (int) Math.round(a / t * 100) : 0;
            avgPercent.put(d, Math.min(200, p));
        }

        // 热门菜谱 Top 3（以打卡次数）
        List<Map<String, Object>> topRecipes = recipeCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(3)
                .map(e -> {
                    Recipe r = recipeMapper.selectById(e.getKey());
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("recipeId", e.getKey());
                    m.put("title", r == null ? "已下架" : r.getTitle());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("stageType", stage);
        data.put("stageLabel", NutritionTarget.stageLabel(stage));
        data.put("days", days);
        data.put("startDate", startDate.toString());
        data.put("endDate", today.toString());
        data.put("checkinCount", actions.size());
        data.put("checkinDays", checkinDates.size());
        data.put("avgActual", roundActual(avgActual));
        data.put("target", target);
        data.put("avgPercent", avgPercent);
        data.put("items", buildReportItems(avgActual, target, avgPercent));
        data.put("topRecipes", topRecipes);
        return Result.ok(data);
    }

    private List<Map<String, Object>> buildReportItems(Map<String, Double> avgActual,
                                                       Map<String, Number> target,
                                                       Map<String, Integer> avgPercent) {
        Map<String, String> LABEL = Map.of(
                "calories", "热量", "protein", "蛋白质", "calcium", "钙",
                "iron", "铁", "vitA", "维A", "vitC", "维C");
        Map<String, String> UNIT = Map.of(
                "calories", "kcal", "protein", "g", "calcium", "mg",
                "iron", "mg", "vitA", "μg", "vitC", "mg");
        List<Map<String, Object>> list = new ArrayList<>();
        for (String d : DIMS) {
            Map<String, Object> it = new LinkedHashMap<>();
            it.put("key", d);
            it.put("name", LABEL.get(d));
            it.put("unit", UNIT.get(d));
            it.put("avgActual", Math.round(avgActual.get(d) * 10) / 10.0);
            it.put("target", target.get(d));
            it.put("avgPercent", avgPercent.get(d));
            list.add(it);
        }
        return list;
    }
}
