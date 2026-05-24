package ai.xiaodudou.module.ai.filter;

import ai.xiaodudou.module.recipe.entity.Ingredient;
import ai.xiaodudou.module.recipe.entity.RecipeIngredient;
import ai.xiaodudou.module.recipe.mapper.IngredientMapper;
import ai.xiaodudou.module.recipe.mapper.RecipeIngredientMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 过敏硬规则二次过滤
 *
 * 设计思想：AI 的 Prompt 可能被绕过/出错，**Java 端必须二次校验**。
 * 规则：菜谱使用的任何食材 ingredient.allergen_tags 含用户 allergies → 整道菜剔除
 *
 * 额外：月子忌口标记 postpartum_taboo=1 的食材，对 POSTPARTUM 阶段用户也剔除
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AllergyFilter {

    private final RecipeIngredientMapper recipeIngredientMapper;
    private final IngredientMapper ingredientMapper;

    /**
     * @param recipeIds 待过滤的菜谱 ID 列表
     * @param allergies 用户过敏标签数组（如 ["egg","peanut"]）
     * @param isPostpartum 是否处于月子阶段（POSTPARTUM）
     * @return 不安全的 recipeId 集合（应从推荐结果中剔除）
     */
    public Set<Long> findUnsafeRecipes(Collection<Long> recipeIds,
                                       List<String> allergies,
                                       boolean isPostpartum) {
        if (recipeIds == null || recipeIds.isEmpty()) return Set.of();

        boolean hasAllergy = allergies != null && !allergies.isEmpty();
        if (!hasAllergy && !isPostpartum) return Set.of();

        // 1. 查这些菜谱的所有食材关联
        List<RecipeIngredient> rels = recipeIngredientMapper.selectList(
                new LambdaQueryWrapper<RecipeIngredient>().in(RecipeIngredient::getRecipeId, recipeIds));

        if (rels.isEmpty()) return Set.of();

        // 2. 查食材主数据
        Set<Long> ingredientIds = new HashSet<>();
        for (RecipeIngredient r : rels) ingredientIds.add(r.getIngredientId());
        List<Ingredient> ingredients = ingredientMapper.selectBatchIds(ingredientIds);
        Map<Long, Ingredient> ingMap = new HashMap<>();
        for (Ingredient i : ingredients) ingMap.put(i.getId(), i);

        // 3. 遍历，命中过敏 / 月子忌口的整道剔除
        Set<Long> unsafe = new HashSet<>();
        for (RecipeIngredient r : rels) {
            Ingredient ing = ingMap.get(r.getIngredientId());
            if (ing == null) continue;

            // 过敏命中
            if (hasAllergy && ing.getAllergenTags() != null) {
                for (String tag : ing.getAllergenTags()) {
                    if (allergies.contains(tag)) {
                        unsafe.add(r.getRecipeId());
                        log.info("[Allergy] 菜谱 {} 含过敏源 {}（食材 {}），剔除", r.getRecipeId(), tag, ing.getName());
                        break;
                    }
                }
            }

            // 月子忌口
            if (isPostpartum && ing.getPostpartumTaboo() != null && ing.getPostpartumTaboo() == 1) {
                unsafe.add(r.getRecipeId());
                log.info("[Allergy] 菜谱 {} 含月子忌口食材 {}，剔除", r.getRecipeId(), ing.getName());
            }
        }
        return unsafe;
    }
}
