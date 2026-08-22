package ai.xiaodudou.module.recipe.dto;

import ai.xiaodudou.module.recipe.entity.Ingredient;

import java.util.List;

/** 食材公开字段；不返回内部营养原始对象和管理字段。 */
public record IngredientResponse(Long id, String name, String category, List<String> allergenTags) {
    public static IngredientResponse from(Ingredient ingredient) {
        return new IngredientResponse(ingredient.getId(), ingredient.getName(), ingredient.getCategory(),
                ingredient.getAllergenTags() == null ? List.of() : List.copyOf(ingredient.getAllergenTags()));
    }
}
