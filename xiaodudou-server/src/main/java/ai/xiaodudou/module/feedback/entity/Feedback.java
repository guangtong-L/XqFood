package ai.xiaodudou.module.feedback.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "t_feedback", autoResultMap = true)
public class Feedback implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private String content;
    private String contact;

    /** general / bug / suggestion / business */
    private String category;

    /** PENDING / PROCESSING / RESOLVED / IGNORED */
    private String status;

    private String reply;
    private LocalDateTime repliedAt;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> clientInfo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    public static final String STATUS_PENDING = "PENDING";
}
