package ai.xiaodudou.module.recipe.dto;

import ai.xiaodudou.module.recipe.entity.RecipeIngredient;

public record RecipeIngredientResponse(Long ingredientId, String quantity, boolean optional) {
    public static RecipeIngredientResponse from(RecipeIngredient relation) {
        return new RecipeIngredientResponse(relation.getIngredientId(), relation.getQuantity(),
                Integer.valueOf(1).equals(relation.getIsOptional()));
    }
}
