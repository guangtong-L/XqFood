package ai.xiaodudou.module.recipe.dto;

import ai.xiaodudou.module.recipe.entity.Recipe;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 菜谱公开字段白名单。 */
public record RecipeResponse(Long id, String title, String coverUrl, Integer cookMinutes,
                             Integer difficulty, List<String> stageTags, Map<String, BigDecimal> nutrition,
                             List<RecipeStepResponse> steps, String description) {
    private static final List<String> NUTRITION_DIMENSIONS =
            List.of("calories", "protein", "calcium", "iron", "vitA", "vitC");

    public static RecipeResponse from(Recipe recipe) {
        return new RecipeResponse(recipe.getId(), recipe.getTitle(), recipe.getCoverUrl(),
                recipe.getCookMinutes(), recipe.getDifficulty(),
                recipe.getStageTags() == null ? List.of() : List.copyOf(recipe.getStageTags()),
                publicNutrition(recipe.getNutrition()), publicSteps(recipe.getSteps()),
                recipe.getDescription());
    }

    private static Map<String, BigDecimal> publicNutrition(Map<String, Object> source) {
        if (source == null) return Map.of();
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (String dimension : NUTRITION_DIMENSIONS) {
            Object value = source.get(dimension);
            if (!(value instanceof Number) && !(value instanceof String)) continue;
            try {
                BigDecimal number = new BigDecimal(value.toString());
                if (number.signum() >= 0) result.put(dimension, number);
            } catch (NumberFormatException ignored) {
                // 非数字的历史 JSON 不进入公开契约。
            }
        }
        return Map.copyOf(result);
    }

    private static List<RecipeStepResponse> publicSteps(List<Map<String, Object>> source) {
        if (source == null) return List.of();
        return source.stream().map(RecipeStepResponse::from).toList();
    }
}
