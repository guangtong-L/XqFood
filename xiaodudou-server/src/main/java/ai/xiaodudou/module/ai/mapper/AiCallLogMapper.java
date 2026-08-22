package ai.xiaodudou.module.ai.mapper;

import ai.xiaodudou.module.ai.entity.AiCallLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface AiCallLogMapper extends BaseMapper<AiCallLog> {
    @Delete("DELETE FROM t_ai_call_log WHERE user_id = #{userId}")
    int physicallyDeleteByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM t_ai_call_log WHERE created_at < #{cutoff}")
    int deleteCreatedBefore(@Param("cutoff") LocalDateTime cutoff);
}
