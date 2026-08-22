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
 * 基于现有食材标签的冲突筛选。标签可能不完整，只能降低已知风险，不能保证安全。
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
     * @return 与用户已记录过敏标签冲突的 recipeId 集合
     */
    public Set<Long> findTagConflicts(Collection<Long> recipeIds, List<String> allergies) {
        if (recipeIds == null || recipeIds.isEmpty()) return Set.of();

        boolean hasAllergy = allergies != null && !allergies.isEmpty();
        if (!hasAllergy) return Set.of();

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

        // 3. 遍历现有标签；缺失标签不能推断为安全。
        Set<Long> conflicts = new HashSet<>();
        for (RecipeIngredient r : rels) {
            Ingredient ing = ingMap.get(r.getIngredientId());
            if (ing == null) continue;

            // 过敏命中
            if (hasAllergy && ing.getAllergenTags() != null) {
                for (String tag : ing.getAllergenTags()) {
                    if (allergies.contains(tag)) {
                        conflicts.add(r.getRecipeId());
                        log.info("[AllergyTag] recipeId={} matchedTag={}", r.getRecipeId(), tag);
                        break;
                    }
                }
            }

        }
        return conflicts;
    }
}
