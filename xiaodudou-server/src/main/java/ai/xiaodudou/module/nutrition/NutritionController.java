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
}
