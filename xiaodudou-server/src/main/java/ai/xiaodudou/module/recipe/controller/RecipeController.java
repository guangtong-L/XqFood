package ai.xiaodudou.module.recipe.controller;

import ai.xiaodudou.common.dto.PageResponse;
import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.common.result.Result;
import ai.xiaodudou.common.result.ResultCode;
import ai.xiaodudou.module.recipe.dto.IngredientResponse;
import ai.xiaodudou.module.recipe.dto.RecipeDetailResponse;
import ai.xiaodudou.module.recipe.dto.RecipeIngredientResponse;
import ai.xiaodudou.module.recipe.dto.RecipeResponse;
import ai.xiaodudou.module.recipe.entity.Ingredient;
import ai.xiaodudou.module.recipe.entity.Recipe;
import ai.xiaodudou.module.recipe.entity.RecipeIngredient;
import ai.xiaodudou.module.recipe.mapper.IngredientMapper;
import ai.xiaodudou.module.recipe.mapper.RecipeIngredientMapper;
import ai.xiaodudou.module.recipe.mapper.RecipeMapper;
import cn.dev33.satoken.annotation.SaIgnore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Validated
@Tag(name = "03 - 食谱")
@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
public class RecipeController {
    private final RecipeMapper recipeMapper;
    private final RecipeIngredientMapper recipeIngredientMapper;
    private final IngredientMapper ingredientMapper;

    @SaIgnore
    @GetMapping
    @Operation(summary = "菜谱分页列表")
    public Result<PageResponse<RecipeResponse>> list(
            @RequestParam(required = false)
            @Pattern(regexp = "^(postpartum_early|postpartum_late|lactation|weaning|child)$",
                    message = "stageTag 不合法") String stageTag,
            @RequestParam(required = false) @Size(max = 50, message = "keyword 不能超过 50 字") String keyword,
            @RequestParam(defaultValue = "1") @Min(1) @Max(10000) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) Integer size) {
        LambdaQueryWrapper<Recipe> query = new LambdaQueryWrapper<Recipe>()
                .eq(Recipe::getStatus, 1).eq(Recipe::getDeleted, 0);
        if (keyword != null && !keyword.isBlank()) query.apply("INSTR(title, {0}) > 0", keyword.trim());
        if (stageTag != null && !stageTag.isBlank()) {
            query.apply("JSON_CONTAINS(stage_tags, JSON_QUOTE({0}))", stageTag);
        }
        query.orderByDesc(Recipe::getId);
        Page<Recipe> result = recipeMapper.selectPage(new Page<>(page, size), query);
        List<RecipeResponse> records = result.getRecords().stream().map(RecipeResponse::from).toList();
        return Result.ok(PageResponse.of(records, result.getTotal(), page, size));
    }

    @SaIgnore
    @GetMapping("/{id}")
    @Operation(summary = "上架菜谱详情")
    public Result<RecipeDetailResponse> detail(@PathVariable @Positive Long id) {
        Recipe recipe = recipeMapper.selectActiveById(id);
        if (recipe == null) throw new BusinessException(ResultCode.RECIPE_NOT_FOUND);
        List<RecipeIngredient> relations = recipeIngredientMapper.selectList(
                new LambdaQueryWrapper<RecipeIngredient>().eq(RecipeIngredient::getRecipeId, id));
        List<Long> ingredientIds = relations.stream().map(RecipeIngredient::getIngredientId).distinct().toList();
        List<Ingredient> ingredients = ingredientIds.isEmpty() ? List.of() : ingredientMapper.selectBatchIds(ingredientIds);
        Map<Long, Ingredient> ingredientMap = new LinkedHashMap<>();
        ingredients.forEach(ingredient -> ingredientMap.put(ingredient.getId(), ingredient));
        List<IngredientResponse> ingredientResponses = ingredientIds.stream()
                .map(ingredientMap::get).filter(java.util.Objects::nonNull).map(IngredientResponse::from).toList();
        return Result.ok(new RecipeDetailResponse(RecipeResponse.from(recipe), ingredientResponses,
                relations.stream().map(RecipeIngredientResponse::from).toList()));
    }
}
