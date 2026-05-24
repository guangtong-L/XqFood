package ai.xiaodudou.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 阶段画像
 */
@Data
@TableName(value = "t_user_profile", autoResultMap = true)
public class UserProfile implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    /** PREPARE / PREGNANCY / POSTPARTUM / WEANING / CHILD */
    private String stageType;
    private Integer pregnancyWeek;
    private Integer postpartumDay;

    /** natural / cesarean */
    private String deliveryType;

    /** breast / mixed / formula */
    private String feedingType;
    private LocalDate babyBirthDate;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> allergies;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> dislikes;

    private String healthNotes;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
