package ai.xiaodudou.module.action.dto;

import java.util.Map;

public record CalendarResponse(String month, int daysInMonth, int checkinDays,
                               int totalCheckins, Map<String, Integer> dayCount) {}
