package ai.xiaodudou.module.action.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户食谱行为：收藏 / 打卡 / 分享
 */
@Data
@TableName("t_user_recipe_action")
public class UserRecipeAction implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private Long recipeId;

    /** favorite / cook / share */
    private String action;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public static final String FAVORITE = "favorite";
    public static final String COOK = "cook";
    public static final String SHARE = "share";
}
