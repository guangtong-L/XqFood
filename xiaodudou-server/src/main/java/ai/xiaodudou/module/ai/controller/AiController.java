package ai.xiaodudou.module.ai.controller;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.common.result.Result;
import ai.xiaodudou.common.result.ResultCode;
import ai.xiaodudou.config.RuntimeModePolicy;
import ai.xiaodudou.module.ai.client.ZhipuClient;
import ai.xiaodudou.module.ai.dto.AiIngredientCategory;
import ai.xiaodudou.module.ai.dto.MissingIngredientResponse;
import ai.xiaodudou.module.ai.dto.RecommendRequest;
import ai.xiaodudou.module.ai.dto.RecognitionResponse;
import ai.xiaodudou.module.ai.dto.RecognizedIngredientResponse;
import ai.xiaodudou.module.ai.dto.RecommendationItemResponse;
import ai.xiaodudou.module.ai.dto.RecommendationResponse;
import ai.xiaodudou.module.ai.filter.AllergyFilter;
import ai.xiaodudou.module.ai.limiter.AiRateLimiter;
import ai.xiaodudou.module.ai.service.AiAuditLogService;
import ai.xiaodudou.module.ai.service.AiFeatureGate;
import ai.xiaodudou.module.ai.service.AiOutputParser;
import ai.xiaodudou.module.ai.service.AiPromptBuilder;
import ai.xiaodudou.module.ai.service.ImageUploadValidator;
import ai.xiaodudou.module.ai.service.ValidatedImage;
import ai.xiaodudou.module.recipe.dto.RecipeResponse;
import ai.xiaodudou.module.recipe.entity.Recipe;
import ai.xiaodudou.module.recipe.mapper.RecipeMapper;
import ai.xiaodudou.module.user.dto.ProfileData;
import ai.xiaodudou.module.user.service.ProfileService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Tag(name = "04 - AI（默认关闭）")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {
    private static final String RECOGNIZE_PROMPT = """
            你是食材图片结构化识别程序，不是医生或营养师。
            只识别图片中可见食材，不提供医疗、营养、母婴阶段或安全建议。
            category只能是：蔬菜、水果、肉禽、海鲜、蛋奶、主食、豆制品、调味料、其他。
            最多输出20项；名称不超过20字；quantityEstimate不超过20字；confidence必须在0到1之间。
            严格输出JSON：{"ingredients":[{"name":"番茄","category":"蔬菜","quantityEstimate":"2个","confidence":0.9,"emoji":"🍅"}]}
            """;
    private static final String DISCLAIMER = "AI 辅助结果仅供食材整理和菜谱浏览参考，不构成医疗或营养建议，请人工核对。";
    private static final String ALLERGY_NOTICE = "仅基于现有食材过敏标签降低已知风险；标签可能不完整，食用前仍需人工核对食材、包装与个人情况。";

    private final RecipeMapper recipeMapper;
    private final ProfileService profileService;
    private final ZhipuClient zhipu;
    private final AiRateLimiter rateLimiter;
    private final AllergyFilter allergyFilter;
    private final AiAuditLogService aiAuditLogService;
    private final RuntimeModePolicy runtimeModePolicy;
    private final AiFeatureGate aiFeatureGate;
    private final ImageUploadValidator imageUploadValidator;
    private final AiOutputParser outputParser;
    private final AiPromptBuilder promptBuilder;

    @GetMapping("/quota")
    @Operation(summary = "查询开发环境AI剩余额度")
    public Result<AiRateLimiter.RemainingQuota> quota() {
        aiFeatureGate.requireAvailable();
        return Result.ok(rateLimiter.getRemaining(StpUtil.getLoginIdAsLong()));
    }

    @PostMapping(value = "/recognize", consumes = "multipart/form-data")
    @Operation(summary = "食材图片识别（默认关闭）")
    public Result<RecognitionResponse> recognize(@RequestPart("image") MultipartFile image) {
        aiFeatureGate.requireAvailable();
        Long userId = StpUtil.getLoginIdAsLong();
        ValidatedImage validatedImage = imageUploadValidator.validate(image);
        AiRateLimiter.QuotaLease quotaLease = rateLimiter.checkAndConsume(userId);
        long startedAt = System.currentTimeMillis();
        String modelVersion = "unavailable";
        int outputCount = 0;
        boolean success = false;
        try {
            BusinessException invalidOutput = null;
            if (zhipu.isEnabled()) {
                try {
                    String text = zhipu.chatWithImage(validatedImage.bytes(), validatedImage.mediaType(), RECOGNIZE_PROMPT);
                    List<RecognizedIngredientResponse> ingredients = outputParser.parseRecognition(text);
                    modelVersion = "glm-4v-plus";
                    outputCount = ingredients.size();
                    success = true;
                    return Result.ok(new RecognitionResponse(UUID.randomUUID().toString(), ingredients,
                            modelVersion, false, "AI辅助识别", DISCLAIMER));
                } catch (BusinessException e) {
                    invalidOutput = e;
                    log.warn("ai_recognize_invalid_output type={}", e.getClass().getSimpleName());
                } catch (Exception e) {
                    log.warn("ai_recognize_upstream_failed type={}", e.getClass().getSimpleName());
                }
            }
            if (!runtimeModePolicy.isMockAiAllowed()) {
                if (invalidOutput != null) throw invalidOutput;
                throw new BusinessException(ResultCode.AI_SERVICE_UNAVAILABLE);
            }
            List<RecognizedIngredientResponse> mock = mockIngredients();
            modelVersion = "mock-fallback";
            outputCount = mock.size();
            success = true;
            return Result.ok(new RecognitionResponse(UUID.randomUUID().toString(), mock,
                    modelVersion, true, "开发模式模拟结果", DISCLAIMER));
        } finally {
            if (!success) rateLimiter.rollback(quotaLease);
            aiAuditLogService.record(userId, "recognize", 1, outputCount, null, modelVersion,
                    (int) (System.currentTimeMillis() - startedAt), success ? 1 : 0);
        }
    }

    @PostMapping("/recommend")
    @Operation(summary = "候选菜谱AI排序（默认关闭）")
    public Result<RecommendationResponse> recommend(@Valid @RequestBody RecommendRequest request) {
        aiFeatureGate.requireAvailable();
        Long userId = StpUtil.getLoginIdAsLong();
        AiRateLimiter.QuotaLease quotaLease = rateLimiter.checkAndConsume(userId);
        long startedAt = System.currentTimeMillis();
        String modelVersion = "unavailable";
        List<RecommendationItemResponse> recommendations = List.of();
        boolean success = false;
        try {
            ProfileData profile = profileService.getData(userId);
            List<String> allergies = profile == null || profile.getAllergies() == null
                    ? List.of() : profile.getAllergies();

            LambdaQueryWrapper<Recipe> query = new LambdaQueryWrapper<Recipe>()
                    .eq(Recipe::getStatus, 1).eq(Recipe::getDeleted, 0)
                    .and(wrapper -> wrapper.isNull(Recipe::getCookMinutes)
                            .or().le(Recipe::getCookMinutes, request.getMaxCookMinutes()))
                    .orderByDesc(Recipe::getId).last("LIMIT 20");
            List<Recipe> candidates = recipeMapper.selectList(query);
            if (!candidates.isEmpty() && !allergies.isEmpty()) {
                Set<Long> conflicts = allergyFilter.findTagConflicts(
                        candidates.stream().map(Recipe::getId).toList(), allergies);
                candidates = candidates.stream().filter(recipe -> !conflicts.contains(recipe.getId())).toList();
            }
            if (candidates.isEmpty()) {
                throw new BusinessException(ResultCode.RECIPE_NOT_FOUND, "暂无符合条件且标签无已知冲突的上架菜谱");
            }

            Map<Long, Recipe> candidateMap = new LinkedHashMap<>();
            candidates.forEach(recipe -> candidateMap.put(recipe.getId(), recipe));
            BusinessException invalidOutput = null;
            if (zhipu.isEnabled()) {
                try {
                    String text = zhipu.chat(AiPromptBuilder.SYSTEM_PROMPT,
                            promptBuilder.build(request, profile, candidates));
                    List<AiOutputParser.ModelRecommendation> parsed = outputParser.parseRecommendations(
                            text, request.getCount(), candidateMap.keySet());
                    recommendations = parsed.stream().map(item -> responseItem(candidateMap.get(item.recipeId()), item)).toList();
                    modelVersion = "glm-4-plus";
                } catch (BusinessException e) {
                    invalidOutput = e;
                    log.warn("ai_recommend_invalid_output type={}", e.getClass().getSimpleName());
                } catch (Exception e) {
                    log.warn("ai_recommend_upstream_failed type={}", e.getClass().getSimpleName());
                }
            }

            if (recommendations.isEmpty()) {
                if (!runtimeModePolicy.isMockAiAllowed()) {
                    if (invalidOutput != null) throw invalidOutput;
                    throw new BusinessException(ResultCode.AI_SERVICE_UNAVAILABLE);
                }
                List<RecommendationItemResponse> fallback = new ArrayList<>();
                int index = 0;
                for (Recipe recipe : candidates) {
                    if (index >= request.getCount()) break;
                    fallback.add(new RecommendationItemResponse(RecipeResponse.from(recipe),
                            Math.max(0, 90 - index * 5), "开发模式模拟排序，仅用于测试接口与页面",
                            List.of()));
                    index++;
                }
                recommendations = List.copyOf(fallback);
                modelVersion = "mock-fallback";
            }
            success = true;
            return Result.ok(new RecommendationResponse(recommendations,
                    "mock-fallback".equals(modelVersion) ? "开发模式模拟结果" : "AI辅助生成",
                    "mock-fallback".equals(modelVersion), DISCLAIMER, ALLERGY_NOTICE));
        } finally {
            if (!success) rateLimiter.rollback(quotaLease);
            List<Long> ids = recommendations.stream().map(item -> item.recipe().id()).distinct().limit(20).toList();
            aiAuditLogService.record(userId, "recommend", request.getIngredients().size(), recommendations.size(), ids,
                    modelVersion, (int) (System.currentTimeMillis() - startedAt), success ? 1 : 0);
        }
    }

    private RecommendationItemResponse responseItem(Recipe recipe, AiOutputParser.ModelRecommendation item) {
        return new RecommendationItemResponse(RecipeResponse.from(recipe), item.matchScore(),
                item.reason(), item.missingIngredients());
    }

    private List<RecognizedIngredientResponse> mockIngredients() {
        return List.of(
                new RecognizedIngredientResponse("番茄", AiIngredientCategory.VEGETABLE, "2个", new BigDecimal("0.95"), "🍅"),
                new RecognizedIngredientResponse("鸡蛋", AiIngredientCategory.EGG_DAIRY, "3个", new BigDecimal("0.92"), "🥚")
        );
    }
}
