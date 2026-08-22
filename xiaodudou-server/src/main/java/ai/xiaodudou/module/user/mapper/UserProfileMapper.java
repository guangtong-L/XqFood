package ai.xiaodudou.module.user.mapper;

import ai.xiaodudou.module.user.entity.UserProfile;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface UserProfileMapper extends BaseMapper<UserProfile> {

    @Select("SELECT * FROM t_user_profile WHERE user_id = #{userId} AND deleted = 0 LIMIT 1")
    UserProfile selectActiveByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT * FROM t_user_profile
            WHERE deleted = 0
              AND encrypted_payload IS NULL
              AND (stage_type IS NOT NULL OR pregnancy_week IS NOT NULL OR postpartum_day IS NOT NULL
                   OR delivery_type IS NOT NULL OR feeding_type IS NOT NULL OR baby_birth_date IS NOT NULL
                   OR allergies IS NOT NULL OR dislikes IS NOT NULL OR health_notes IS NOT NULL)
            ORDER BY id
            LIMIT #{limit}
            """)
    List<UserProfile> selectLegacyBatch(@Param("limit") int limit);

    @Update("""
            UPDATE t_user_profile
            SET encrypted_payload = #{encryptedPayload},
                encryption_key_version = #{keyVersion},
                stage_type = NULL,
                pregnancy_week = NULL,
                postpartum_day = NULL,
                delivery_type = NULL,
                feeding_type = NULL,
                baby_birth_date = NULL,
                allergies = NULL,
                dislikes = NULL,
                health_notes = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id} AND deleted = 0
            """)
    int updateEncryptedAndClearLegacy(@Param("id") Long id,
                                      @Param("encryptedPayload") String encryptedPayload,
                                      @Param("keyVersion") String keyVersion);

    @Delete("DELETE FROM t_user_profile WHERE user_id = #{userId}")
    int physicallyDeleteByUserId(@Param("userId") Long userId);
}
