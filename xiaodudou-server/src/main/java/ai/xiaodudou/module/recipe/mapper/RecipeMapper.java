package ai.xiaodudou.module.recipe.mapper;

import ai.xiaodudou.module.recipe.entity.Recipe;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface RecipeMapper extends BaseMapper<Recipe> {
    @Select("SELECT * FROM t_recipe WHERE id = #{id} AND status = 1 AND deleted = 0 LIMIT 1")
    Recipe selectActiveById(@Param("id") Long id);
}
