package ai.xiaodudou.module.nutrition;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 历史阶段目标占位模型，未经适用人群、年龄与专业口径验收。
 *
 * <p>生产接口不得引用本类，也不得据此生成达标率、缺口或健康建议。
 * 保留代码仅用于兼容历史版本，待数据口径正式验收后再决定替换或删除。</p>
 */
@Deprecated(forRemoval = true)
public class NutritionTarget {

    public static Map<String, Number> forStage(String stageType) {
        if (stageType == null) return TARGET_DEFAULT;
        return switch (stageType) {
            case "PREGNANCY"   -> TARGET_PREGNANCY;
            case "POSTPARTUM"  -> TARGET_POSTPARTUM;  // 哺乳期
            case "WEANING"     -> TARGET_WEANING;
            case "CHILD"       -> TARGET_CHILD;
            default            -> TARGET_DEFAULT;
        };
    }

    /** 哺乳期女性 - 营养需求最高 */
    private static final Map<String, Number> TARGET_POSTPARTUM = build(
            2300, 80, 1000, 24, 1300, 130);

    /** 孕中晚期 */
    private static final Map<String, Number> TARGET_PREGNANCY = build(
            2100, 75, 1000, 29, 770, 115);

    /** 辅食期妈妈（实际是给宝宝做辅食的妈妈，按通用） */
    private static final Map<String, Number> TARGET_WEANING = build(
            1800, 55, 800, 20, 700, 100);

    /** 儿童 1-6 岁（妈妈仍是录入主体，但目标看孩子） */
    private static final Map<String, Number> TARGET_CHILD = build(
            1300, 30, 600, 10, 350, 50);

    /** 默认（备孕等） */
    private static final Map<String, Number> TARGET_DEFAULT = build(
            1800, 55, 800, 20, 700, 100);

    private static Map<String, Number> build(int kcal, int protein, int calcium,
                                              int iron, int vitA, int vitC) {
        Map<String, Number> m = new LinkedHashMap<>();
        m.put("calories", kcal);
        m.put("protein", protein);
        m.put("calcium", calcium);
        m.put("iron", iron);
        m.put("vitA", vitA);
        m.put("vitC", vitC);
        return m;
    }

    /** 阶段中文名（前端展示） */
    public static String stageLabel(String stageType) {
        if (stageType == null) return "通用";
        return switch (stageType) {
            case "PREPARE" -> "备孕";
            case "PREGNANCY" -> "孕期";
            case "POSTPARTUM" -> "哺乳期";
            case "WEANING" -> "辅食期";
            case "CHILD" -> "儿童期";
            default -> stageType;
        };
    }
}
