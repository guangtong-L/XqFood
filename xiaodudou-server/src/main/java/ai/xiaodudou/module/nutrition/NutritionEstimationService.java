package ai.xiaodudou.module.nutrition;

import ai.xiaodudou.module.action.entity.UserRecipeAction;
import ai.xiaodudou.module.action.mapper.UserRecipeActionMapper;
import ai.xiaodudou.module.recipe.entity.Recipe;
import ai.xiaodudou.module.recipe.mapper.RecipeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NutritionEstimationService {
    private static final List<String> DIMENSIONS = List.of("calories", "protein", "calcium", "iron", "vitA", "vitC");
    private static final BigDecimal MAX_PER_SERVING = new BigDecimal("100000");
    private static final BigDecimal MAX_TOTAL = new BigDecimal("1000000");
    private final UserRecipeActionMapper actionMapper;
    private final RecipeMapper recipeMapper;

    public NutritionTodayResponse today(Long userId) {
        LocalDate today = LocalDate.now();
        List<UserRecipeAction> actions = actionMapper.selectOwnCookByDate(userId, today);
        List<Long> recipeIds = actions.stream().map(UserRecipeAction::getRecipeId).distinct().toList();
        Map<Long, Recipe> recipes = new HashMap<>();
        if (!recipeIds.isEmpty()) recipeMapper.selectBatchIds(recipeIds).forEach(r -> recipes.put(r.getId(), r));

        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        BigDecimal servingsTotal = BigDecimal.ZERO;
        int includedEntries = 0;
        for (UserRecipeAction action : actions) {
            BigDecimal servings = action.getServings();
            if (servings == null || servings.compareTo(new BigDecimal("0.25")) < 0
                    || servings.compareTo(new BigDecimal("10")) > 0) {
                log.warn("nutrition_estimate_skip actionId={} reason=invalid_servings", action.getId());
                continue;
            }
            servingsTotal = servingsTotal.add(servings);
            Recipe recipe = recipes.get(action.getRecipeId());
            if (recipe == null || recipe.getNutrition() == null) continue;
            boolean anyValid = false;
            for (String dimension : DIMENSIONS) {
                BigDecimal perServing = number(recipe.getNutrition().get(dimension));
                if (perServing == null || perServing.signum() < 0 || perServing.compareTo(MAX_PER_SERVING) > 0) {
                    if (recipe.getNutrition().containsKey(dimension)) {
                        log.warn("nutrition_estimate_skip recipeId={} dimension={} reason=invalid_value",
                                recipe.getId(), dimension);
                    }
                    continue;
                }
                BigDecimal contribution = perServing.multiply(servings);
                BigDecimal next = totals.getOrDefault(dimension, BigDecimal.ZERO).add(contribution);
                if (next.compareTo(MAX_TOTAL) > 0) {
                    log.warn("nutrition_estimate_skip recipeId={} dimension={} reason=total_limit",
                            recipe.getId(), dimension);
                    continue;
                }
                totals.put(dimension, next);
                anyValid = true;
            }
            if (anyValid) {
                includedEntries++;
            }
        }
        Map<String, BigDecimal> rounded = new LinkedHashMap<>();
        totals.forEach((key, value) -> rounded.put(key, value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros()));
        return new NutritionTodayResponse(today, actions.size(), includedEntries,
                servingsTotal.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros(), rounded, true,
                "仅按已记录菜谱的每份营养数据乘以用户填写份数汇总",
                "unverified",
                "估算仅覆盖已记录且有有效营养数据的菜谱，不代表全天摄入，也不用于判断是否达到膳食参考摄入量。");
    }

    private BigDecimal number(Object value) {
        if (value == null) return null;
        try {
            if (value instanceof Number || value instanceof String) return new BigDecimal(value.toString());
        } catch (NumberFormatException ignored) {
            // 由调用方记录维度级警告。
        }
        return null;
    }
}
