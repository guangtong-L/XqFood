package ai.xiaodudou.module.action.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CheckinRequest(
        @NotNull(message = "recipeId 不能为空") @Positive(message = "recipeId 必须为正数") Long recipeId,
        @NotNull(message = "mealType 不能为空")
        @Pattern(regexp = "^(breakfast|lunch|dinner|snack)$", message = "mealType 不合法") String mealType,
        @NotNull(message = "servings 不能为空")
        @DecimalMin(value = "0.25", message = "servings 不能小于 0.25")
        @DecimalMax(value = "10", message = "servings 不能大于 10")
        @Digits(integer = 2, fraction = 2, message = "servings 最多保留两位小数") BigDecimal servings,
        LocalDate actionDate
) {}
