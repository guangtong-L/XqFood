package ai.xiaodudou.module.action.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CheckinItemResponse(Long actionId, Long recipeId, String title, String coverUrl,
                                  String mealType, BigDecimal servings, LocalDate actionDate,
                                  LocalDateTime checkedAt) {}
