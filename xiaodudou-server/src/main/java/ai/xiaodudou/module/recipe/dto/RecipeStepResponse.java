package ai.xiaodudou.module.recipe.dto;

import java.util.Map;

/** 菜谱步骤公开字段白名单。 */
public record RecipeStepResponse(Integer step, String desc, Integer timer) {
    static RecipeStepResponse from(Map<String, Object> source) {
        return new RecipeStepResponse(integer(source.get("step")), text(source.get("desc")), integer(source.get("timer")));
    }

    private static Integer integer(Object value) {
        if (value instanceof Number number) return number.intValue();
        return null;
    }

    private static String text(Object value) {
        return value instanceof String text ? text : null;
    }
}
