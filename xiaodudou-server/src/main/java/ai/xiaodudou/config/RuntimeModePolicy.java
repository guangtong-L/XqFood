package ai.xiaodudou.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 统一运行模式策略。Mock 能力必须同时满足“dev/local 环境 + 显式开关”。
 */
@Component
public class RuntimeModePolicy {

    private final Environment environment;

    @Value("${xiaodudou.mock.login-enabled:false}")
    private boolean mockLoginEnabled;

    @Value("${xiaodudou.mock.ai-enabled:false}")
    private boolean mockAiEnabled;

    public RuntimeModePolicy(Environment environment) {
        this.environment = environment;
    }

    public boolean isDevelopmentProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "dev".equals(profile) || "local".equals(profile));
    }

    public boolean isMockLoginAllowed() {
        return isDevelopmentProfile() && mockLoginEnabled;
    }

    public boolean isMockAiAllowed() {
        return isDevelopmentProfile() && mockAiEnabled;
    }
}
