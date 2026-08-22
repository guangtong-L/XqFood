package ai.xiaodudou.module.recipe;

import ai.xiaodudou.common.dto.PageResponse;
import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.common.result.Result;
import ai.xiaodudou.module.recipe.controller.RecipeController;
import ai.xiaodudou.module.recipe.dto.RecipeResponse;
import ai.xiaodudou.module.recipe.entity.Recipe;
import ai.xiaodudou.module.recipe.mapper.IngredientMapper;
import ai.xiaodudou.module.recipe.mapper.RecipeIngredientMapper;
import ai.xiaodudou.module.recipe.mapper.RecipeMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.executable.ExecutableValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecipeContractTest {
    private RecipeMapper recipeMapper;
    private RecipeController controller;

    @BeforeEach
    void setUp() {
        recipeMapper = mock(RecipeMapper.class);
        controller = new RecipeController(recipeMapper, mock(RecipeIngredientMapper.class), mock(IngredientMapper.class));
    }

    @Test
    void publicRecipeDtoDoesNotExposePersistenceOrManagementFields() throws Exception {
        Recipe recipe = new Recipe();
        recipe.setId(1L);
        recipe.setTitle("测试菜谱");
        recipe.setStatus(1);
        recipe.setDeleted(0);
        recipe.setCreatedAt(LocalDateTime.now());
        recipe.setUpdatedAt(LocalDateTime.now());
        recipe.setNutrition(Map.of("calories", 120, "internalCost", 99));
        recipe.setSteps(List.of(Map.of("step", 1, "desc", "完成", "timer", 60, "adminNote", "内部")));

        String json = new ObjectMapper().writeValueAsString(RecipeResponse.from(recipe));

        assertThat(json).contains("测试菜谱")
                .contains("calories", "完成")
                .doesNotContain("status", "deleted", "createdAt", "updatedAt", "internalCost", "adminNote");
        Method list = RecipeController.class.getMethod("list", String.class, String.class, Integer.class, Integer.class);
        Method detail = RecipeController.class.getMethod("detail", Long.class);
        assertThat(list.getGenericReturnType().getTypeName()).doesNotContain(".entity.");
        assertThat(detail.getGenericReturnType().getTypeName()).doesNotContain(".entity.");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void listUsesExactJsonMembershipAndStablePageEnvelope() throws Exception {
        Page<Recipe> returned = new Page<>(2, 10, 21);
        Recipe recipe = new Recipe();
        recipe.setId(1L);
        recipe.setTitle("测试");
        returned.setRecords(List.of(recipe));
        when(recipeMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(returned);

        Result<PageResponse<RecipeResponse>> result = controller.list("lactation", "蛋", 2, 10);

        ArgumentCaptor<Wrapper<Recipe>> wrapper = ArgumentCaptor.forClass(Wrapper.class);
        verify(recipeMapper).selectPage(any(Page.class), wrapper.capture());
        assertThat(wrapper.getValue()).isNotNull();
        String source = Files.readString(Path.of(
                "src/main/java/ai/xiaodudou/module/recipe/controller/RecipeController.java"));
        assertThat(source).contains("JSON_CONTAINS(stage_tags, JSON_QUOTE({0}))")
                .doesNotContain("stage_tags LIKE");
        assertThat(result.getData().page()).isEqualTo(2);
        assertThat(result.getData().size()).isEqualTo(10);
        assertThat(result.getData().total()).isEqualTo(21);
        assertThat(result.getData().pages()).isEqualTo(3);
    }

    @Test
    void methodValidationRejectsUnknownTagAndPaginationBoundaries() throws Exception {
        ExecutableValidator validator = Validation.buildDefaultValidatorFactory().getValidator().forExecutables();
        Method method = RecipeController.class.getMethod("list", String.class, String.class, Integer.class, Integer.class);

        assertThat(validator.validateParameters(controller, method, new Object[]{"lact", "", 0, 51}))
                .hasSize(3);
    }

    @Test
    void detailOnlyUsesActiveRecipeLookup() {
        when(recipeMapper.selectActiveById(8L)).thenReturn(null);
        assertThatThrownBy(() -> controller.detail(8L))
                .isInstanceOf(BusinessException.class);
        verify(recipeMapper).selectActiveById(8L);
    }
}
