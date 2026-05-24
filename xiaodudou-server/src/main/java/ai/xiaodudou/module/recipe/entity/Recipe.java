package ai.xiaodudou.module.recipe.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 食谱
 */
@Data
@TableName(value = "t_recipe", autoResultMap = true)
public class Recipe implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String title;
    private String coverUrl;
    private Integer cookMinutes;
    private Integer difficulty;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> stageTags;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> nutrition;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> steps;

    private String description;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
