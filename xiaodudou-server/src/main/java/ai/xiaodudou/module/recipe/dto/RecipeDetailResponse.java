package ai.xiaodudou.module.recipe.dto;

import java.util.List;

public record RecipeDetailResponse(RecipeResponse recipe, List<IngredientResponse> ingredients,
                                   List<RecipeIngredientResponse> recipeIngredients) {}
