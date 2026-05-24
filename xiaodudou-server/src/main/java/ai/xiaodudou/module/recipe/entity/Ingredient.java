package ai.xiaodudou.module.recipe.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "t_ingredient", autoResultMap = true)
public class Ingredient implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> alias;

    private String category;

    @TableField(value = "nutrition_per_100g", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> nutritionPer100g;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> allergenTags;

    private Integer postpartumTaboo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
