package ai.xiaodudou.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StartupSafetyValidatorTest {

    @Test
    void prodShouldRejectMocksAndMissingRealConfiguration() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("xiaodudou.mock.login-enabled", "true")
                .withProperty("xiaodudou.mock.ai-enabled", "true")
                .withProperty("xiaodudou.commercial.enabled", "true");
        env.setActiveProfiles("prod", "dev");

        assertThatThrownBy(() -> StartupSafetyValidator.validate(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("生产安全闸门拒绝启动")
                .hasMessageContaining("禁止 Mock 登录")
                .hasMessageContaining("禁止 Mock AI")
                .hasMessageContaining("商业化功能尚未完成")
                .hasMessageContaining("spring.datasource.url");
    }

    @Test
    void prodShouldStartOnlyWhenAllSafetyRequirementsAreMet() {
        MockEnvironment env = validProdEnvironment();
        assertThatCode(() -> StartupSafetyValidator.validate(env)).doesNotThrowAnyException();
    }

    @Test
    void prodShouldRejectWildcardOrInsecureCorsOrigin() {
        MockEnvironment env = validProdEnvironment()
                .withProperty("xiaodudou.cors.allowed-origins", "*");

        assertThatThrownBy(() -> StartupSafetyValidator.validate(env))
                .hasMessageContaining("携带凭据时禁止使用通配来源");
    }

    @Test
    void anyProfileShouldRejectWildcardCorsWhenCredentialsAreEnabled() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("xiaodudou.cors.allow-credentials", "true")
                .withProperty("xiaodudou.cors.allowed-origins", "*");
        env.setActiveProfiles("dev");

        assertThatThrownBy(() -> StartupSafetyValidator.validate(env))
                .hasMessageContaining("携带凭据时禁止使用通配来源");
    }

    @Test
    void prodShouldRejectInvalidEncryptionKeyAndRetentionDays() {
        MockEnvironment env = validProdEnvironment()
                .withProperty("xiaodudou.security.data-encryption-key", "not-base64")
                .withProperty("xiaodudou.ai.log-retention-days", "366");

        assertThatThrownBy(() -> StartupSafetyValidator.validate(env))
                .hasMessageContaining("合法 Base64")
                .hasMessageContaining("1..365");
    }

    @Test
    void prodShouldRejectCommunityEvenWhenOtherSettingsAreSafe() {
        MockEnvironment env = validProdEnvironment()
                .withProperty("xiaodudou.features.community-enabled", "true");

        assertThatThrownBy(() -> StartupSafetyValidator.validate(env))
                .hasMessageContaining("妈妈圈缺少发布同意机制");
    }

    @Test
    void prodShouldRejectUnverifiedNutritionReportEvenWhenOtherSettingsAreSafe() {
        MockEnvironment env = validProdEnvironment()
                .withProperty("xiaodudou.features.nutrition-report-enabled", "true");

        assertThatThrownBy(() -> StartupSafetyValidator.validate(env))
                .hasMessageContaining("营养目标百分比与N日报告尚未完成验证");
    }

    @Test
    void prodShouldRejectAiFeatureEvenIfModelCredentialsExist() {
        MockEnvironment env = validProdEnvironment()
                .withProperty("xiaodudou.features.ai-enabled", "true");

        assertThatThrownBy(() -> StartupSafetyValidator.validate(env))
                .hasMessageContaining("真实内容安全审核供应商尚未完成接入验收");
    }

    @Test
    void prodShouldRejectApiDocsAndUnsafeSessionOrLoginLimits() {
        MockEnvironment env = validProdEnvironment()
                .withProperty("springdoc.api-docs.enabled", "true")
                .withProperty("sa-token.timeout", "604801")
                .withProperty("sa-token.active-timeout", "-1")
                .withProperty("xiaodudou.rate-limit.login-attempts", "0");

        assertThatThrownBy(() -> StartupSafetyValidator.validate(env))
                .hasMessageContaining("OpenAPI")
                .hasMessageContaining("最长有效期")
                .hasMessageContaining("无操作有效期")
                .hasMessageContaining("登录限流次数");
    }

    private MockEnvironment validProdEnvironment() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("xiaodudou.mock.login-enabled", "false")
                .withProperty("xiaodudou.mock.ai-enabled", "false")
                .withProperty("xiaodudou.commercial.enabled", "false")
                .withProperty("xiaodudou.features.community-enabled", "false")
                .withProperty("xiaodudou.wechat.pay.enabled", "false")
                .withProperty("xiaodudou.wechat.miniapp.enabled", "true")
                .withProperty("xiaodudou.wechat.miniapp.appid", "wx-production-appid")
                .withProperty("xiaodudou.wechat.miniapp.secret", "configured")
                .withProperty("spring.datasource.url", "jdbc:mysql://db.internal/xiaodudou")
                .withProperty("spring.datasource.username", "configured")
                .withProperty("spring.datasource.password", "configured")
                .withProperty("spring.data.redis.host", "redis.internal")
                .withProperty("spring.data.redis.password", "configured")
                .withProperty("xiaodudou.security.data-encryption-key",
                        Base64.getEncoder().encodeToString(new byte[32]))
                .withProperty("xiaodudou.ai.log-retention-days", "30")
                .withProperty("sa-token.timeout", "604800")
                .withProperty("sa-token.active-timeout", "43200")
                .withProperty("xiaodudou.rate-limit.login-attempts", "10")
                .withProperty("xiaodudou.rate-limit.login-window-seconds", "60")
                .withProperty("xiaodudou.cors.allowed-origins", "https://app.example.com");
        env.setActiveProfiles("prod");
        return env;
    }
}
