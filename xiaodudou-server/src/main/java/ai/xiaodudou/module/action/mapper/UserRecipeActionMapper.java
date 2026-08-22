package ai.xiaodudou.module.action.mapper;

import ai.xiaodudou.module.action.entity.UserRecipeAction;
import ai.xiaodudou.module.recipe.entity.Recipe;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface UserRecipeActionMapper extends BaseMapper<UserRecipeAction> {
    @Delete("DELETE FROM t_user_recipe_action WHERE user_id = #{userId}")
    int physicallyDeleteByUserId(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO t_user_recipe_action
              (id, user_id, recipe_id, action, action_date, meal_type, servings, idempotency_key, created_at)
            VALUES
              (#{id}, #{userId}, #{recipeId}, #{action}, #{actionDate}, #{mealType}, #{servings}, #{idempotencyKey}, #{createdAt})
            """)
    int insertAction(@Param("id") Long id, @Param("userId") Long userId,
                     @Param("recipeId") Long recipeId, @Param("action") String action,
                     @Param("actionDate") LocalDate actionDate, @Param("mealType") String mealType,
                     @Param("servings") BigDecimal servings, @Param("idempotencyKey") String idempotencyKey,
                     @Param("createdAt") LocalDateTime createdAt);

    /** 打卡仍以唯一键处理并发，服务层会在 affectedRows=0 后严格核验同幂等记录。 */
    @Insert("""
            INSERT IGNORE INTO t_user_recipe_action
              (id, user_id, recipe_id, action, action_date, meal_type, servings, idempotency_key, created_at)
            VALUES
              (#{id}, #{userId}, #{recipeId}, #{action}, #{actionDate}, #{mealType}, #{servings}, #{idempotencyKey}, #{createdAt})
            """)
    int insertIgnoreAction(@Param("id") Long id, @Param("userId") Long userId,
                           @Param("recipeId") Long recipeId, @Param("action") String action,
                           @Param("actionDate") LocalDate actionDate, @Param("mealType") String mealType,
                           @Param("servings") BigDecimal servings, @Param("idempotencyKey") String idempotencyKey,
                           @Param("createdAt") LocalDateTime createdAt);

    @Select("""
            SELECT * FROM t_user_recipe_action
            WHERE user_id=#{userId} AND action=#{action} AND idempotency_key=#{idempotencyKey}
            LIMIT 1
            """)
    UserRecipeAction selectOwnByIdempotency(@Param("userId") Long userId,
                                             @Param("action") String action,
                                             @Param("idempotencyKey") String idempotencyKey);

    @Delete("""
            DELETE FROM t_user_recipe_action
            WHERE user_id=#{userId} AND recipe_id=#{recipeId} AND action='favorite'
            """)
    int deleteOwnFavorite(@Param("userId") Long userId, @Param("recipeId") Long recipeId);

    @Select("""
            SELECT COUNT(*) FROM t_user_recipe_action
            WHERE user_id=#{userId} AND recipe_id=#{recipeId} AND action='favorite'
            """)
    int countOwnFavorite(@Param("userId") Long userId, @Param("recipeId") Long recipeId);

    @Select("""
            SELECT r.* FROM t_user_recipe_action a
            JOIN t_recipe r ON r.id=a.recipe_id AND r.status=1 AND r.deleted=0
            WHERE a.user_id=#{userId} AND a.action='favorite'
            ORDER BY a.created_at DESC, a.id DESC
            """)
    Page<Recipe> selectOwnFavoriteRecipes(Page<Recipe> page, @Param("userId") Long userId);

    @Select("""
            SELECT * FROM t_user_recipe_action
            WHERE user_id=#{userId} AND action='cook' AND action_date=#{actionDate}
            ORDER BY created_at DESC, id DESC
            """)
    List<UserRecipeAction> selectOwnCookByDate(@Param("userId") Long userId,
                                                @Param("actionDate") LocalDate actionDate);

    @Delete("""
            DELETE FROM t_user_recipe_action
            WHERE id=#{actionId} AND user_id=#{userId} AND action='cook'
            """)
    int deleteOwnCook(@Param("userId") Long userId, @Param("actionId") Long actionId);

    @Select("""
            SELECT action_date AS day, COUNT(*) AS count
            FROM t_user_recipe_action
            WHERE user_id=#{userId} AND action='cook'
              AND action_date >= #{start} AND action_date < #{end}
            GROUP BY action_date ORDER BY action_date
            """)
    List<Map<String, Object>> aggregateCookByDay(@Param("userId") Long userId,
                                                  @Param("start") LocalDate start,
                                                  @Param("end") LocalDate end);
}
