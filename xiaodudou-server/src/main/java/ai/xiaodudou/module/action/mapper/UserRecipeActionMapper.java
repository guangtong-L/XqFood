package ai.xiaodudou.module.action.mapper;

import ai.xiaodudou.module.action.entity.UserRecipeAction;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface UserRecipeActionMapper extends BaseMapper<UserRecipeAction> {

    /**
     * 月度打卡聚合：返回 [{day:'2026-05-23', count:3}, ...]
     */
    @Select("""
            SELECT DATE(created_at) AS day, COUNT(*) AS count
            FROM t_user_recipe_action
            WHERE user_id = #{userId}
              AND action = 'cook'
              AND created_at >= #{start}
              AND created_at < #{end}
            GROUP BY DATE(created_at)
            ORDER BY day
            """)
    List<Map<String, Object>> aggregateCookByDay(@Param("userId") Long userId,
                                                  @Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);
}
