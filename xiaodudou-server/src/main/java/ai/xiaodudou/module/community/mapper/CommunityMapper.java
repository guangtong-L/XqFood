package ai.xiaodudou.module.community.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 妈妈圈聚合查询（跨表 JOIN，单独写 Mapper）
 */
@Mapper
public interface CommunityMapper {

    /**
     * 同阶段妈妈最近打卡流
     * - 排除自己（不让自己看见自己）
     * - 只看 24 小时内的打卡（保持新鲜感）
     */
    @Select("""
            SELECT
                ura.id          AS actionId,
                ura.created_at  AS checkedAt,
                u.id            AS userId,
                u.nickname      AS nickname,
                u.avatar_url    AS avatarUrl,
                p.stage_type    AS stageType,
                p.postpartum_day AS postpartumDay,
                p.pregnancy_week AS pregnancyWeek,
                r.id            AS recipeId,
                r.title         AS recipeTitle,
                r.cover_url     AS recipeCover
            FROM t_user_recipe_action ura
                INNER JOIN t_user u ON u.id = ura.user_id AND u.deleted = 0
                INNER JOIN t_user_profile p ON p.user_id = ura.user_id AND p.deleted = 0
                INNER JOIN t_recipe r ON r.id = ura.recipe_id AND r.deleted = 0
            WHERE ura.action = 'cook'
              AND ura.created_at >= NOW() - INTERVAL 1 DAY
              AND ura.user_id != #{currentUserId}
              AND p.stage_type = #{stageType}
            ORDER BY ura.created_at DESC
            LIMIT #{size} OFFSET #{offset}
            """)
    List<Map<String, Object>> sameStageRecentFeed(@Param("currentUserId") Long currentUserId,
                                                  @Param("stageType") String stageType,
                                                  @Param("size") int size,
                                                  @Param("offset") int offset);

    /**
     * 同阶段最近 24 小时打卡总数（用于"还有 N 位妈妈在这一阶段"展示）
     */
    @Select("""
            SELECT COUNT(DISTINCT ura.user_id)
            FROM t_user_recipe_action ura
                INNER JOIN t_user_profile p ON p.user_id = ura.user_id AND p.deleted = 0
            WHERE ura.action = 'cook'
              AND ura.created_at >= NOW() - INTERVAL 1 DAY
              AND ura.user_id != #{currentUserId}
              AND p.stage_type = #{stageType}
            """)
    int sameStageActiveUserCount(@Param("currentUserId") Long currentUserId,
                                  @Param("stageType") String stageType);
}
