package ai.xiaodudou.module.ai.service;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.common.result.ResultCode;
import ai.xiaodudou.module.ai.dto.AiIngredientCategory;
import ai.xiaodudou.module.ai.dto.MissingIngredientResponse;
import ai.xiaodudou.module.ai.dto.RecognizedIngredientResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class AiOutputParser {
    private static final int MAX_MODEL_TEXT = 64 * 1024;
    private static final Pattern INGREDIENT_NAME = Pattern.compile("^[\\p{L}\\p{N}（）()·\\- ]+$");
    private final ObjectMapper objectMapper;

    public List<RecognizedIngredientResponse> parseRecognition(String text) {
        JsonNode root = root(text);
        JsonNode items = root.get("ingredients");
        if (items == null || !items.isArray() || items.isEmpty() || items.size() > 20) invalid();
        List<RecognizedIngredientResponse> result = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (JsonNode item : items) {
            String name = boundedText(item, "name", 1, 20, true);
            if (!INGREDIENT_NAME.matcher(name).matches() || !names.add(name)) invalid();
            AiIngredientCategory category;
            try {
                category = AiIngredientCategory.from(boundedText(item, "category", 1, 8, true));
            } catch (IllegalArgumentException e) {
                throw invalidException();
            }
            String quantity = nullableText(item, "quantityEstimate", 20);
            BigDecimal confidence = decimal(item.get("confidence"));
            if (confidence == null || confidence.compareTo(BigDecimal.ZERO) < 0
                    || confidence.compareTo(BigDecimal.ONE) > 0) invalid();
            String emoji = nullableText(item, "emoji", 8);
            result.add(new RecognizedIngredientResponse(name, category, quantity,
                    confidence.stripTrailingZeros(), emoji));
        }
        return List.copyOf(result);
    }

    public List<ModelRecommendation> parseRecommendations(String text, int limit, Set<Long> candidateIds) {
        if (limit < 1 || limit > 5 || candidateIds == null) invalid();
        JsonNode root = root(text);
        JsonNode items = root.get("recommendations");
        if (items == null || !items.isArray() || items.isEmpty() || items.size() > limit) invalid();
        List<ModelRecommendation> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (JsonNode item : items) {
            JsonNode idNode = item.get("recipeId");
            if (idNode == null || !idNode.isIntegralNumber() || !idNode.canConvertToLong()) invalid();
            long recipeId = idNode.longValue();
            if (!candidateIds.contains(recipeId) || !seen.add(recipeId)) invalid();
            JsonNode scoreNode = item.get("matchScore");
            if (scoreNode == null || !scoreNode.isIntegralNumber()) invalid();
            int score = scoreNode.intValue();
            if (score < 0 || score > 100) invalid();
            String reason = boundedText(item, "reason", 1, 80, false);
            JsonNode missingNode = item.get("missingIngredients");
            if (missingNode == null || !missingNode.isArray() || missingNode.size() > 10) invalid();
            List<MissingIngredientResponse> missing = new ArrayList<>();
            for (JsonNode missingItem : missingNode) {
                String name = boundedText(missingItem, "name", 1, 20, true);
                if (!INGREDIENT_NAME.matcher(name).matches()) invalid();
                missing.add(new MissingIngredientResponse(name, nullableText(missingItem, "quantity", 20)));
            }
            result.add(new ModelRecommendation(recipeId, score, reason, List.copyOf(missing)));
        }
        return List.copyOf(result);
    }

    private JsonNode root(String text) {
        if (text == null || text.isBlank() || text.length() > MAX_MODEL_TEXT) throw invalidException();
        String normalized = text.trim();
        if (normalized.startsWith("```")) {
            int start = normalized.indexOf('\n');
            int end = normalized.lastIndexOf("```");
            if (start < 0 || end <= start) throw invalidException();
            normalized = normalized.substring(start + 1, end).trim();
        }
        try {
            JsonNode root = objectMapper.readTree(normalized);
            if (root == null || !root.isObject()) throw invalidException();
            return root;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw invalidException();
        }
    }

    private String boundedText(JsonNode owner, String field, int min, int max, boolean trim) {
        JsonNode value = owner == null ? null : owner.get(field);
        if (value == null || !value.isTextual()) throw invalidException();
        String text = normalize(value.textValue(), trim);
        if (text.length() < min || text.length() > max) throw invalidException();
        return text;
    }

    private String nullableText(JsonNode owner, String field, int max) {
        JsonNode value = owner == null ? null : owner.get(field);
        if (value == null || value.isNull()) return null;
        if (!value.isTextual()) throw invalidException();
        String text = normalize(value.textValue(), true);
        if (text.length() > max) throw invalidException();
        return text.isBlank() ? null : text;
    }

    private String normalize(String value, boolean trim) {
        String normalized = value.replaceAll("[\\r\\n\\t]+", " ").replaceAll(" {2,}", " ");
        return trim ? normalized.trim() : normalized.trim();
    }

    private BigDecimal decimal(JsonNode value) {
        if (value == null || !value.isNumber()) return null;
        try { return value.decimalValue(); } catch (ArithmeticException e) { return null; }
    }

    private void invalid() { throw invalidException(); }

    private BusinessException invalidException() {
        return new BusinessException(ResultCode.AI_INVALID_RESPONSE, "AI 返回结构异常，请稍后重试");
    }

    public record ModelRecommendation(long recipeId, int matchScore, String reason,
                                      List<MissingIngredientResponse> missingIngredients) {}
}
