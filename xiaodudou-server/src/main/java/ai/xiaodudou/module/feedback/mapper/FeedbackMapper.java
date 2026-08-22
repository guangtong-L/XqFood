package ai.xiaodudou.module.feedback.mapper;

import ai.xiaodudou.module.feedback.entity.Feedback;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {
    @Delete("DELETE FROM t_feedback WHERE user_id = #{userId}")
    int physicallyDeleteByUserId(@Param("userId") Long userId);
}
