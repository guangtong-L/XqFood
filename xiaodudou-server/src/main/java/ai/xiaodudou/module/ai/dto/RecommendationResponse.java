package ai.xiaodudou.module.ai.dto;

import java.util.List;

public record RecommendationResponse(List<RecommendationItemResponse> recommendations,
                                     String aiLabel, boolean fallback, String disclaimer,
                                     String allergyNotice) {}
