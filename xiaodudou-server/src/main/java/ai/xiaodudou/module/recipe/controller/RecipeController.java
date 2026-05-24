package ai.xiaodudou.module.recipe.controller;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.common.result.Result;
import ai.xiaodudou.common.result.ResultCode;
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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public Result<Map<String, Object>> list(
            @Parameter(description = "阶段标签 lactation/weaning 等") @RequestParam(required = false) String stageTag,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        LambdaQueryWrapper<Recipe> qw = new LambdaQueryWrapper<>();
        qw.eq(Recipe::getStatus, 1);
        if (keyword != null && !keyword.isBlank()) qw.like(Recipe::getTitle, keyword);
        // JSON 字段查询：用 LIKE 简化（生产环境用 JSON_CONTAINS）
        if (stageTag != null && !stageTag.isBlank()) qw.like(Recipe::getStageTags, stageTag);
        qw.orderByDesc(Recipe::getId);

        Page<Recipe> p = recipeMapper.selectPage(new Page<>(page, size), qw);
        Map<String, Object> data = new HashMap<>();
        data.put("records", p.getRecords());
        data.put("total", p.getTotal());
        data.put("page", page);
        data.put("size", size);
        return Result.ok(data);
    }

    @SaIgnore
    @GetMapping("/{id}")
    @Operation(summary = "菜谱详情（含食材关联）")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Recipe recipe = recipeMapper.selectById(id);
        if (recipe == null) throw new BusinessException(ResultCode.RECIPE_NOT_FOUND);

        List<RecipeIngredient> ris = recipeIngredientMapper.selectList(
                new LambdaQueryWrapper<RecipeIngredient>().eq(RecipeIngredient::getRecipeId, id));

        List<Long> ingredientIds = ris.stream().map(RecipeIngredient::getIngredientId).toList();
        List<Ingredient> ingredients = ingredientIds.isEmpty()
                ? List.of()
                : ingredientMapper.selectBatchIds(ingredientIds);

        Map<String, Object> data = new HashMap<>();
        data.put("recipe", recipe);
        data.put("ingredients", ingredients);
        data.put("recipeIngredients", ris);
        return Result.ok(data);
    }
}
