package ai.xiaodudou.module.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * 会员套餐（服务端定价，前端只传 code，金额不可由前端决定）
 *
 * ⚠️ 金额单位：分（避免浮点精度）
 */
@Getter
@AllArgsConstructor
public enum VipPackage {

    POSTPARTUM_CARD("POSTPARTUM_CARD", "月子精养卡", 59900, 1, 90,
            "AI 调用 50 次/日 · 哺乳期专属菜谱 · 营养师咨询 3 次"),

    WEANING_YEAR("WEANING_YEAR", "辅食年卡", 39900, 2, 365,
            "AI 调用 50 次/日 · 辅食月龄阶梯 · 过敏排查档案"),

    CHILD_YEAR("CHILD_YEAR", "儿童年卡", 29900, 3, 365,
            "AI 调用 50 次/日 · 长高营养追踪 · 入园便当方案");

    /** 套餐编码（数据库 / API 传递） */
    private final String code;
    /** 显示名 */
    private final String name;
    /** 价格（分） */
    private final int amountFen;
    /** 会员等级（写入 t_user.vip_level） */
    private final int vipLevel;
    /** 有效天数 */
    private final int validDays;
    /** 权益描述（一句话） */
    private final String benefits;

    public static VipPackage of(String code) {
        return Arrays.stream(values()).filter(p -> p.code.equals(code)).findFirst().orElse(null);
    }

    public static List<VipPackage> all() {
        return Arrays.asList(values());
    }
}
