package ai.xiaodudou.module.nutrition;

import ai.xiaodudou.module.action.entity.UserRecipeAction;
import ai.xiaodudou.module.action.mapper.UserRecipeActionMapper;
import ai.xiaodudou.module.recipe.entity.Recipe;
import ai.xiaodudou.module.recipe.mapper.RecipeMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NutritionEstimationServiceTest {

    @Test
    void multipliesPerServingNutritionAndDoesNotInventMissingDimensions() {
        UserRecipeActionMapper actionMapper = mock(UserRecipeActionMapper.class);
        RecipeMapper recipeMapper = mock(RecipeMapper.class);
        UserRecipeAction action = action(1L, 10L, "1.50");
        when(actionMapper.selectOwnCookByDate(7L, LocalDate.now())).thenReturn(List.of(action));
        Recipe recipe = recipe(10L, Map.of("calories", 200, "protein", "12.5"));
        when(recipeMapper.selectBatchIds(List.of(10L))).thenReturn(List.of(recipe));

        NutritionTodayResponse response = new NutritionEstimationService(actionMapper, recipeMapper).today(7L);

        assertThat(response.recordedEntries()).isOne();
        assertThat(response.includedEntries()).isOne();
        assertThat(response.recordedServings()).isEqualByComparingTo("1.5");
        assertThat(response.estimatedNutrition().get("calories")).isEqualByComparingTo("300");
        assertThat(response.estimatedNutrition().get("protein")).isEqualByComparingTo("18.75");
        assertThat(response.estimatedNutrition()).doesNotContainKeys("calcium", "iron", "vitA", "vitC");
        assertThat(response.estimated()).isTrue();
        assertThat(response.dataQuality()).isEqualTo("unverified");
        assertThat(response.disclaimer()).contains("不代表全天摄入").contains("不用于判断");
    }

    @Test
    void ignoresNegativeOrExtremeNutritionButKeepsTruthfulRecordedServingCount() {
        UserRecipeActionMapper actionMapper = mock(UserRecipeActionMapper.class);
        RecipeMapper recipeMapper = mock(RecipeMapper.class);
        when(actionMapper.selectOwnCookByDate(7L, LocalDate.now()))
                .thenReturn(List.of(action(1L, 10L, "2"), action(2L, 11L, "1")));
        when(recipeMapper.selectBatchIds(List.of(10L, 11L))).thenReturn(List.of(
                recipe(10L, Map.of("calories", -1, "protein", "not-a-number")),
                recipe(11L, Map.of("calories", 100001))));

        NutritionTodayResponse response = new NutritionEstimationService(actionMapper, recipeMapper).today(7L);

        assertThat(response.recordedEntries()).isEqualTo(2);
        assertThat(response.recordedServings()).isEqualByComparingTo("3");
        assertThat(response.includedEntries()).isZero();
        assertThat(response.estimatedNutrition()).isEmpty();
    }

    private UserRecipeAction action(Long id, Long recipeId, String servings) {
        UserRecipeAction action = new UserRecipeAction();
        action.setId(id);
        action.setRecipeId(recipeId);
        action.setServings(new BigDecimal(servings));
        action.setActionDate(LocalDate.now());
        return action;
    }

    private Recipe recipe(Long id, Map<String, Object> nutrition) {
        Recipe recipe = new Recipe();
        recipe.setId(id);
        recipe.setNutrition(nutrition);
        return recipe;
    }
}
