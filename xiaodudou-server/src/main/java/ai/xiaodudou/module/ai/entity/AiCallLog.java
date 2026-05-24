package ai.xiaodudou.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 调用日志（合规留存 ≥ 180 天）
 * 表 t_ai_call_log 已在 V1__init_schema.sql 建好
 */
@Data
@TableName(value = "t_ai_call_log", autoResultMap = true)
public class AiCallLog implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private String endpoint;        // recognize / recommend
    private String inputHash;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object inputPayload;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object outputPayload;

    private String modelVersion;
    private Integer costMs;
    private Integer costTokens;
    private Integer auditStatus;    // 1 通过 / 0 拦截
    private Integer status;         // 1 成功 / 0 失败
    private LocalDateTime createdAt;
}
