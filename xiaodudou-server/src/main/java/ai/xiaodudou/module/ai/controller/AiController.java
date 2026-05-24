package ai.xiaodudou.module.ai.controller;

import ai.xiaodudou.common.result.Result;
import ai.xiaodudou.module.ai.client.ZhipuClient;
import ai.xiaodudou.module.ai.entity.AiCallLog;
import ai.xiaodudou.module.ai.filter.AllergyFilter;
import ai.xiaodudou.module.ai.limiter.AiRateLimiter;
import ai.xiaodudou.module.ai.mapper.AiCallLogMapper;
import ai.xiaodudou.module.recipe.entity.Recipe;
import ai.xiaodudou.module.recipe.mapper.RecipeMapper;
import ai.xiaodudou.module.user.entity.UserProfile;
import ai.xiaodudou.module.user.mapper.UserProfileMapper;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AI 接口（含 限流 + 过敏硬规则 + 调用日志 三层防护）
 */
@Slf4j
@Tag(name = "04 - AI")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final RecipeMapper recipeMapper;
    private final UserProfileMapper userProfileMapper;
    private final ZhipuClient zhipu;
    private final AiRateLimiter rateLimiter;
    private final AllergyFilter allergyFilter;
    private final AiCallLogMapper aiCallLogMapper;

    private static final String RECOGNIZE_PROMPT = """
            你是一名专业食材识别助手。请分析图片中的所有可见生鲜食材，严格按下述 JSON Schema 输出。

            规则：
            1. 只识别可食用的生鲜食材；忽略包装/餐具/桌面/已做好的成品菜。
            2. name 必须用中文标准名（番茄/西红柿 统一为"番茄"）。
            3. category 取值：蔬菜 / 水果 / 肉禽 / 海鲜 / 蛋奶 / 主食 / 豆制品 / 调味料 / 其他。
            4. 置信度 < 0.6 的不输出。
            5. quantityEstimate 给粗略估算，例如"2 个""200g""一把"。无法判断填 null。
            6. emoji 给该食材的代表 emoji。
            7. 不输出任何医疗 / 营养建议。
            8. 直接输出 JSON 对象，不要任何 markdown 标记。

            输出 schema：
            {
              "ingredients": [
                {"name":"string","category":"枚举","quantityEstimate":"string|null","confidence":0.0-1.0,"emoji":"string"}
              ]
            }
            """;

    // ============ 查询额度（前端展示）============
    @GetMapping("/quota")
    @Operation(summary = "查询当前用户 AI 剩余额度")
    public Result<AiRateLimiter.RemainingQuota> quota() {
        return Result.ok(rateLimiter.getRemaining(StpUtil.getLoginIdAsLong()));
    }

    // ============ 食材识别 ============
    @PostMapping(value = "/recognize", consumes = "multipart/form-data")
    @Operation(summary = "食材识别（限流 + 调用日志）")
    public Result<Map<String, Object>> recognize(
            @RequestPart("image") MultipartFile image,
            @RequestParam(required = false) String stageHint) {

        Long userId = StpUtil.getLoginIdAsLong();
        long t0 = System.currentTimeMillis();
        String modelVersion = "mock-fallback";
        Integer status = 1;
        Object outputForLog = null;

        // 1. 限流（超限直接抛异常，不进业务、不落日志）
        rateLimiter.checkAndConsume(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("requestId", UUID.randomUUID().toString());

        try {
            if (zhipu.isEnabled()) {
                try {
                    byte[] bytes = image.getBytes();
                    String text = zhipu.chatWithImage(bytes, RECOGNIZE_PROMPT);
                    JSONObject parsed = parseJsonLoose(text);
                    JSONArray arr = parsed.getJSONArray("ingredients");
                    if (arr != null) {
                        data.put("ingredients", arr);
                        data.put("lowConfidenceCount", 0);
                        data.put("modelVersion", "glm-4v-plus");
                        modelVersion = "glm-4v-plus";
                        outputForLog = arr;
                        return Result.ok(data);
                    }
                    log.warn("[AI] 视觉模型返回 JSON 不含 ingredients 字段，降级 mock");
                } catch (IOException e) {
                    log.error("读取上传图片失败", e);
                    rateLimiter.rollback(userId);  // 系统问题，回滚额度
                    status = 0;
                } catch (Exception e) {
                    log.error("[AI] 智谱视觉识别失败，降级 mock: {}", e.getMessage());
                    // 注意：智谱失败不回滚额度——用户已发起请求，配额按"尝试"计
                }
            }

            // ===== Mock 兜底 =====
            data.put("ingredients", mockIngredients());
            data.put("lowConfidenceCount", 0);
            data.put("modelVersion", "mock-fallback");
            outputForLog = data.get("ingredients");
            return Result.ok(data);
        } finally {
            logAiCall(userId, "recognize", null, outputForLog, modelVersion,
                    (int) (System.currentTimeMillis() - t0), status);
        }
    }

    // ============ 推荐 ============
    @Data
    public static class RecommendReq {
        private Map<String, Object> stage;
        private List<Map<String, Object>> ingredients;
        private Map<String, Object> constraints;
        private Integer count = 3;
    }

    @PostMapping("/recommend")
    @Operation(summary = "菜谱推荐（限流 + 过敏过滤 + 调用日志）")
    public Result<Map<String, Object>> recommend(@RequestBody RecommendReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        long t0 = System.currentTimeMillis();
        String modelVersion = "mock-fallback";
        Integer status = 1;

        // 1. 限流
        rateLimiter.checkAndConsume(userId);

        // 2. 读用户阶段画像（用于过敏过滤）
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId));
        List<String> userAllergies = profile != null && profile.getAllergies() != null
                ? profile.getAllergies() : List.of();
        boolean isPostpartum = profile != null && "POSTPARTUM".equals(profile.getStageType());

        // 3. 召回候选
        List<Recipe> candidates = recipeMapper.selectList(
                new LambdaQueryWrapper<Recipe>()
                        .eq(Recipe::getStatus, 1)
                        .last("limit 20"));

        // 3.1 ⚠️ 召回阶段就过滤掉不安全的菜谱（双层保险，AI 之前先过滤）
        if (!candidates.isEmpty()) {
            List<Long> candidateIds = candidates.stream().map(Recipe::getId).toList();
            Set<Long> unsafe = allergyFilter.findUnsafeRecipes(candidateIds, userAllergies, isPostpartum);
            if (!unsafe.isEmpty()) {
                int before = candidates.size();
                candidates = candidates.stream().filter(r -> !unsafe.contains(r.getId())).toList();
                log.info("[AI] 召回阶段过敏过滤：{} → {} (剔除 {} 道)", before, candidates.size(), unsafe.size());
            }
        }

        Map<Long, Recipe> candidateMap = new HashMap<>();
        for (Recipe r : candidates) candidateMap.put(r.getId(), r);

        int count = req.getCount() == null ? 3 : req.getCount();
        List<Map<String, Object>> recommendations = new ArrayList<>();

        try {
            if (zhipu.isEnabled() && !candidates.isEmpty()) {
                try {
                    String userPrompt = buildRecommendPrompt(req, candidates, count);
                    String text = zhipu.chat(
                            "你是一名中国注册营养师，擅长母婴营养。严格按 JSON schema 输出，不要任何 markdown 标记。",
                            userPrompt);
                    JSONObject parsed = parseJsonLoose(text);
                    JSONArray arr = parsed.getJSONArray("recommendations");
                    if (arr != null && !arr.isEmpty()) {
                        for (Object o : arr) {
                            JSONObject item = (JSONObject) o;
                            Long rid = item.getLong("recipeId");
                            Recipe r = candidateMap.get(rid);
                            if (r == null) continue;
                            Map<String, Object> rec = new HashMap<>();
                            rec.put("recipeId", r.getId());
                            rec.put("title", r.getTitle());
                            rec.put("coverUrl", r.getCoverUrl());
                            rec.put("matchScore", item.getInt("matchScore", 80));
                            rec.put("reason", item.getStr("reason", ""));
                            rec.put("nutrition", r.getNutrition());
                            rec.put("cookMinutes", r.getCookMinutes());
                            rec.put("stageTags", r.getStageTags());
                            rec.put("missingIngredients", item.getJSONArray("missingIngredients"));
                            recommendations.add(rec);
                        }
                        modelVersion = "glm-4-plus";
                    }
                } catch (Exception e) {
                    log.error("[AI] 智谱推荐失败，降级 mock: {}", e.getMessage());
                }
            }

            // 4. ⚠️ AI 结果二次过滤（防 AI 出错把过敏菜推给用户）
            if (!recommendations.isEmpty()) {
                List<Long> recIds = recommendations.stream()
                        .map(m -> Long.valueOf(m.get("recipeId").toString())).toList();
                Set<Long> unsafe = allergyFilter.findUnsafeRecipes(recIds, userAllergies, isPostpartum);
                if (!unsafe.isEmpty()) {
                    int before = recommendations.size();
                    recommendations = recommendations.stream()
                            .filter(m -> !unsafe.contains(Long.valueOf(m.get("recipeId").toString())))
                            .toList();
                    log.warn("[AI] AI 返回结果含过敏菜谱，二次过滤剔除 {} 道", before - recommendations.size());
                }
            }

            // 5. Mock 兜底（含已过滤的候选）
            if (recommendations.isEmpty()) {
                List<Map<String, Object>> fallback = new ArrayList<>();
                int idx = 0;
                for (Recipe r : candidates) {
                    if (idx >= count) break;
                    Map<String, Object> rec = new HashMap<>();
                    rec.put("recipeId", r.getId());
                    rec.put("title", r.getTitle());
                    rec.put("coverUrl", r.getCoverUrl());
                    rec.put("matchScore", 90 - idx * 5);
                    rec.put("reason", "适合当前阶段，营养均衡（降级 Mock）");
                    rec.put("nutrition", r.getNutrition());
                    rec.put("cookMinutes", r.getCookMinutes());
                    rec.put("stageTags", r.getStageTags());
                    rec.put("missingIngredients", List.of());
                    fallback.add(rec);
                    idx++;
                }
                recommendations = fallback;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("recommendations", recommendations);
            data.put("disclaimer", "AI 推荐仅供参考，特殊体质请咨询营养师或医生");
            return Result.ok(data);
        } finally {
            String inputHash = DigestUtil.md5Hex(JSONUtil.toJsonStr(req));
            logAiCall(userId, "recommend", req, recommendations, modelVersion,
                    (int) (System.currentTimeMillis() - t0), status);
        }
    }

    // ============ 工具方法 ============

    /** 异步可改，M1 同步写日志（量小） */
    private void logAiCall(Long userId, String endpoint, Object input, Object output,
                           String modelVersion, int costMs, Integer status) {
        try {
            AiCallLog log = new AiCallLog();
            log.setUserId(userId);
            log.setEndpoint(endpoint);
            if (input != null) {
                log.setInputHash(DigestUtil.md5Hex(JSONUtil.toJsonStr(input)));
                log.setInputPayload(input);
            }
            log.setOutputPayload(output);
            log.setModelVersion(modelVersion);
            log.setCostMs(costMs);
            log.setAuditStatus(1);  // M1 暂无内容审核，先标记通过
            log.setStatus(status);
            log.setCreatedAt(LocalDateTime.now());
            aiCallLogMapper.insert(log);
        } catch (Exception e) {
            // 日志失败不能影响业务
            log.error("[AiCallLog] 落库失败 userId={} endpoint={}: {}", userId, endpoint, e.getMessage());
        }
    }

    private String buildRecommendPrompt(RecommendReq req, List<Recipe> candidates, int count) {
        StringBuilder sb = new StringBuilder();
        sb.append("【用户阶段】\n").append(JSONUtil.toJsonStr(req.getStage())).append("\n\n");
        sb.append("【现有食材】\n").append(JSONUtil.toJsonStr(req.getIngredients())).append("\n\n");
        sb.append("【限制条件】\n").append(JSONUtil.toJsonStr(req.getConstraints())).append("\n\n");

        sb.append("【候选食谱】（只能从中选，已剔除过敏菜）\n");
        for (Recipe r : candidates) {
            sb.append("- id=").append(r.getId())
              .append(" 菜名=").append(r.getTitle())
              .append(" 标签=").append(r.getStageTags())
              .append(" 耗时=").append(r.getCookMinutes()).append("分钟\n");
        }

        sb.append("\n【任务】\n")
          .append("从候选食谱中，按以下规则选 ").append(count).append(" 道最适合的菜：\n")
          .append("1. 食材匹配度 40 分：与现有食材覆盖度越高分越高\n")
          .append("2. 阶段适配度 30 分：与用户阶段（如哺乳期/月子）营养需求匹配\n")
          .append("3. 烹饪可行性 20 分：时间不超过 constraints.maxCookMinutes\n")
          .append("4. 偏好 10 分：避开 constraints.dislikes\n\n")
          .append("严格按下面 JSON 输出，不要 markdown：\n")
          .append("""
                  {
                    "recommendations": [
                      {"recipeId": int, "matchScore": 0-100, "reason": "≤60字说明为什么适合", "missingIngredients": [{"name":"...","quantity":"..."}]}
                    ]
                  }
                  """);
        return sb.toString();
    }

    private JSONObject parseJsonLoose(String text) {
        if (StrUtil.isBlank(text)) return new JSONObject();
        String t = text.trim();
        if (t.startsWith("```")) {
            int s = t.indexOf('\n');
            int e = t.lastIndexOf("```");
            if (s > 0 && e > s) t = t.substring(s + 1, e).trim();
        }
        return JSONUtil.parseObj(t);
    }

    private List<Map<String, Object>> mockIngredients() {
        return List.of(
                Map.of("name", "番茄", "category", "蔬菜", "quantityEstimate", "2 个", "confidence", 0.95, "emoji", "🍅"),
                Map.of("name", "鸡蛋", "category", "蛋奶", "quantityEstimate", "3 个", "confidence", 0.92, "emoji", "🥚"),
                Map.of("name", "白菜", "category", "蔬菜", "quantityEstimate", "200g", "confidence", 0.88, "emoji", "🥬"),
                Map.of("name", "胡萝卜", "category", "蔬菜", "quantityEstimate", "1 根", "confidence", 0.85, "emoji", "🥕")
        );
    }
}
