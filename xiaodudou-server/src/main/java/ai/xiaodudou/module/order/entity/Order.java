package ai.xiaodudou.module.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName(value = "t_order", autoResultMap = true)
public class Order implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String outTradeNo;
    private Long userId;
    private String packageCode;
    private String packageName;
    private Integer amountFen;
    private Integer vipLevel;
    private Integer validDays;

    /** PENDING / PAID / CANCELLED / REFUNDED / EXPIRED */
    private String status;

    private String payChannel;
    private String payTransactionId;
    private LocalDateTime paidAt;
    private LocalDateTime expireAt;
    private LocalDateTime refundedAt;
    private String refundReason;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extra;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_REFUNDED = "REFUNDED";
    public static final String STATUS_EXPIRED = "EXPIRED";
}
