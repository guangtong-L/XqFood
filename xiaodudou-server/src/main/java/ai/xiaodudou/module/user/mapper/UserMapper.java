package ai.xiaodudou.module.user.mapper;

import ai.xiaodudou.module.user.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM t_user WHERE wx_openid = #{openid} LIMIT 1")
    User selectByOpenidIncludingDeleted(@Param("openid") String openid);

    @Select("SELECT * FROM t_user WHERE id = #{userId} LIMIT 1")
    User selectByIdIncludingDeleted(@Param("userId") Long userId);

    @Select("SELECT * FROM t_user WHERE id = #{userId} LIMIT 1 FOR UPDATE")
    User selectByIdForUpdateIncludingDeleted(@Param("userId") Long userId);

    @Update("""
            UPDATE t_user
            SET wx_openid = #{anonymousOpenid},
                wx_unionid = #{anonymousUnionid},
                nickname = NULL,
                avatar_url = NULL,
                phone = NULL,
                status = 0,
                vip_level = 0,
                vip_expire_at = NULL,
                deleted = 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{userId} AND deleted = 0
            """)
    int anonymizeAndDelete(@Param("userId") Long userId,
                           @Param("anonymousOpenid") String anonymousOpenid,
                           @Param("anonymousUnionid") String anonymousUnionid);
}
