package ai.xiaodudou.module.ai.controller;

import ai.xiaodudou.common.result.Result;
import ai.xiaodudou.module.ai.client.ZhipuClient;
import ai.xiaodudou.module.recipe.entity.Recipe;
import ai.xiaodudou.module.recipe.mapper.RecipeMapper;
import cn.hutool.core.util.StrUtil;
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
import java.util.*;

/**
 * AI 接口
 * - 接入智谱 GLM-4V（视觉）+ GLM-4（文本）
 * - 失败/未启用时自动降级到 Mock
 */
@Slf4j
@Tag(name = "04 - AI")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final RecipeMapper recipeMapper;
    private final ZhipuClient zhipu;

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

    @PostMapping(value = "/recognize", consumes = "multipart/form-data")
    @Operation(summary = "食材识别（智谱 GLM-4V，失败降级 Mock）")
    public Result<Map<String, Object>> recognize(
            @RequestPart("image") MultipartFile image,
            @RequestParam(required = false) String stageHint) {

        Map<String, Object> data = new HashMap<>();
        data.put("requestId", UUID.randomUUID().toString());

        if (zhipu.isEnabled()) {
            try {
                byte[] bytes = image.getBytes();
                String text = zhipu.chatWithImage(bytes, RECOGNIZE_PROMPT);
                JSONObject parsed = parseJsonLoose(text);
                JSONArray arr = parsed.getJSONArray("ingredients");
                // 智谱调用成功就用真实结果（空数组也算成功——代表图里真没食材）
                if (arr != null) {
                    data.put("ingredients", arr);
                    data.put("lowConfidenceCount", 0);
                    data.put("modelVersion", "glm-4v-plus");
                    if (arr.isEmpty()) {
                        log.info("[AI] 视觉模型未识别到食材（图中无生鲜食材）");
                    }
                    return Result.ok(data);
                }
                log.warn("[AI] 视觉模型返回 JSON 不含 ingredients 字段，降级 mock。原始: {}", text);
            } catch (IOException e) {
                log.error("读取上传图片失败", e);
            } catch (Exception e) {
                log.error("[AI] 智谱视觉识别失败，降级 mock: {}", e.getMessage());
            }
        }

        // ===== Mock 兜底 =====
        data.put("ingredients", mockIngredients());
        data.put("lowConfidenceCount", 0);
        data.put("modelVersion", "mock-fallback");
        return Result.ok(data);
    }

    @Data
    public static class RecommendReq {
        private Map<String, Object> stage;
        private List<Map<String, Object>> ingredients;
        private Map<String, Object> constraints;
        private Integer count = 3;
    }

    @PostMapping("/recommend")
    @Operation(summary = "菜谱推荐（智谱 GLM-4 文本，失败降级 Mock）")
    public Result<Map<String, Object>> recommend(@RequestBody RecommendReq req) {
        // 召回候选：库内符合阶段 + 已上架的食谱（M1 用全部前 20 道，等向量库再升级）
        List<Recipe> candidates = recipeMapper.selectList(
                new LambdaQueryWrapper<Recipe>()
                        .eq(Recipe::getStatus, 1)
                        .last("limit 20"));

        Map<Long, Recipe> candidateMap = new HashMap<>();
        for (Recipe r : candidates) candidateMap.put(r.getId(), r);

        Integer count = req.getCount() == null ? 3 : req.getCount();
        List<Map<String, Object>> recommendations = new ArrayList<>();

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
                }
            } catch (Exception e) {
                log.error("[AI] 智谱推荐失败，降级 mock: {}", e.getMessage());
            }
        }

        // ===== Mock 兜底 =====
        if (recommendations.isEmpty()) {
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
                recommendations.add(rec);
                idx++;
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("recommendations", recommendations);
        data.put("disclaimer", "AI 推荐仅供参考，特殊体质请咨询营养师或医生");
        return Result.ok(data);
    }

    /** 构造推荐 Prompt */
    private String buildRecommendPrompt(RecommendReq req, List<Recipe> candidates, int count) {
        StringBuilder sb = new StringBuilder();
        sb.append("【用户阶段】\n").append(JSONUtil.toJsonStr(req.getStage())).append("\n\n");
        sb.append("【现有食材】\n").append(JSONUtil.toJsonStr(req.getIngredients())).append("\n\n");
        sb.append("【限制条件】\n").append(JSONUtil.toJsonStr(req.getConstraints())).append("\n\n");

        sb.append("【候选食谱】（只能从中选）\n");
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
          .append("4. 偏好 10 分：避开 constraints.allergies 和 dislikes\n\n")
          .append("硬规则（一票否决）：含过敏源、月子忌口（生冷/酒/辛辣）、超时 → 不要选。\n\n")
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

    /** 容忍模型偶尔返回 markdown 包裹 */
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
