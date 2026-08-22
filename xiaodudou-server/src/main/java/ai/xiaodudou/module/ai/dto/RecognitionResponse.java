package ai.xiaodudou.module.ai.dto;

import java.util.List;

public record RecognitionResponse(String requestId, List<RecognizedIngredientResponse> ingredients,
                                  String modelVersion, boolean fallback, String aiLabel,
                                  String disclaimer) {}
