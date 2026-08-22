package ai.xiaodudou.module.nutrition;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record NutritionTodayResponse(LocalDate date, int recordedEntries, int includedEntries,
                                     BigDecimal recordedServings, Map<String, BigDecimal> estimatedNutrition,
                                     boolean estimated, String basis, String dataQuality, String disclaimer) {}
