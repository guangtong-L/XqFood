package ai.xiaodudou.module.ai.service;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.common.result.ResultCode;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/** AI 总闸门；真实内容安全审核供应商完成接入验收前，生产环境始终不可用。 */
@Component
public class AiFeatureGate {
    private final Environment environment;

    public AiFeatureGate(Environment environment) {
        this.environment = environment;
    }

    public boolean isAvailable() {
        boolean development = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "dev".equals(profile) || "local".equals(profile));
        return development && environment.getProperty("xiaodudou.features.ai-enabled", Boolean.class, false);
    }

    public void requireAvailable() {
        if (!isAvailable()) {
            throw new BusinessException(ResultCode.FEATURE_NOT_AVAILABLE,
                    "AI 功能暂未开放，请先使用菜谱浏览、收藏和打卡");
        }
    }
}
