package ai.xiaodudou.module.ai.dto;

import java.math.BigDecimal;

public record RecognizedIngredientResponse(String name, AiIngredientCategory category,
                                           String quantityEstimate, BigDecimal confidence, String emoji) {}
