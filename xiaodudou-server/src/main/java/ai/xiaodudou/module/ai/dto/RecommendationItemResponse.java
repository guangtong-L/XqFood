package ai.xiaodudou.module.ai.dto;

import ai.xiaodudou.module.recipe.dto.RecipeResponse;

import java.util.List;

public record RecommendationItemResponse(RecipeResponse recipe, int matchScore, String reason,
                                         List<MissingIngredientResponse> missingIngredients) {}
