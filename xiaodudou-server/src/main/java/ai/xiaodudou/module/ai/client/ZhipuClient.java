package ai.xiaodudou.module.ai.client;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 智谱 AI 客户端 (GLM-4V / GLM-4)
 *
 * 文档: https://open.bigmodel.cn/dev/api/normal-model/glm-4
 *
 * 鉴权: v4 接口直接用 API Key 作 Bearer Token
 */
@Slf4j
@Component
public class ZhipuClient {

    @Value("${xiaodudou.ai.zhipu.api-key:}")
    private String apiKey;

    @Value("${xiaodudou.ai.zhipu.base-url:https://open.bigmodel.cn/api/paas/v4}")
    private String baseUrl;

    @Value("${xiaodudou.ai.zhipu.model-vision:glm-4v-plus}")
    private String modelVision;

    @Value("${xiaodudou.ai.zhipu.model-chat:glm-4-plus}")
    private String modelChat;

    @Value("${xiaodudou.ai.zhipu.timeout-ms:15000}")
    private int timeoutMs;

    @Value("${xiaodudou.ai.zhipu.enabled:false}")
    private boolean enabled;

    public boolean isEnabled() {
        return enabled && StrUtil.isNotBlank(apiKey) && !apiKey.startsWith("your-");
    }

    /**
     * 视觉对话：传入图片字节 + Prompt，返回模型文本
     */
    public String chatWithImage(byte[] imageBytes, String prompt) {
        String imageB64 = Base64.encode(imageBytes);
        String dataUrl = "data:image/jpeg;base64," + imageB64;

        JSONObject body = new JSONObject();
        body.set("model", modelVision);

        JSONArray content = new JSONArray();
        content.add(JSONUtil.createObj()
                .set("type", "image_url")
                .set("image_url", JSONUtil.createObj().set("url", dataUrl)));
        content.add(JSONUtil.createObj()
                .set("type", "text")
                .set("text", prompt));

        body.set("messages", JSONUtil.createArray().put(
                JSONUtil.createObj().set("role", "user").set("content", content)));
        body.set("temperature", 0.1);
        body.set("max_tokens", 1024);

        return doCall(body);
    }

    /**
     * 文本对话：传 prompt 返回模型文本
     */
    public String chat(String systemPrompt, String userPrompt) {
        JSONObject body = new JSONObject();
        body.set("model", modelChat);

        JSONArray messages = JSONUtil.createArray();
        if (StrUtil.isNotBlank(systemPrompt)) {
            messages.add(JSONUtil.createObj().set("role", "system").set("content", systemPrompt));
        }
        messages.add(JSONUtil.createObj().set("role", "user").set("content", userPrompt));
        body.set("messages", messages);
        body.set("temperature", 0.3);
        body.set("max_tokens", 2048);

        return doCall(body);
    }

    private String doCall(JSONObject body) {
        long start = System.currentTimeMillis();
        try (HttpResponse resp = HttpRequest.post(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(timeoutMs)
                .body(body.toString())
                .execute()) {

            String respBody = resp.body();
            long cost = System.currentTimeMillis() - start;
            log.info("[Zhipu] cost={}ms status={} bodyLen={}", cost, resp.getStatus(), respBody.length());

            if (!resp.isOk()) {
                log.error("[Zhipu] HTTP {}: {}", resp.getStatus(), respBody);
                throw new RuntimeException("智谱 API 调用失败: HTTP " + resp.getStatus());
            }

            JSONObject json = JSONUtil.parseObj(respBody);
            JSONArray choices = json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("智谱返回为空");
            }
            return choices.getJSONObject(0)
                    .getJSONObject("message")
                    .getStr("content", "");
        } catch (Exception e) {
            log.error("[Zhipu] 调用异常 cost={}ms", System.currentTimeMillis() - start, e);
            throw new RuntimeException("AI 服务不可用: " + e.getMessage(), e);
        }
    }
}
