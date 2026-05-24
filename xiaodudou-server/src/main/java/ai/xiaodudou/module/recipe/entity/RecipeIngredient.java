package ai.xiaodudou.module.recipe.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_recipe_ingredient")
public class RecipeIngredient {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long recipeId;
    private Long ingredientId;
    private String quantity;
    private Integer isOptional;
}
