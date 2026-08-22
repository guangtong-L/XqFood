package ai.xiaodudou.module.ai.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum AiIngredientCategory {
    VEGETABLE("蔬菜"), FRUIT("水果"), MEAT("肉禽"), SEAFOOD("海鲜"),
    EGG_DAIRY("蛋奶"), STAPLE("主食"), SOY("豆制品"), SEASONING("调味料"), OTHER("其他");

    private final String label;

    AiIngredientCategory(String label) { this.label = label; }

    @JsonValue
    public String label() { return label; }

    @JsonCreator
    public static AiIngredientCategory from(String value) {
        return Arrays.stream(values()).filter(item -> item.label.equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知食材分类"));
    }
}
