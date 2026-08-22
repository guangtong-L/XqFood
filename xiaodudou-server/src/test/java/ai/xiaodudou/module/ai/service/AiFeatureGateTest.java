package ai.xiaodudou.module.ai.service;

import ai.xiaodudou.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiFeatureGateTest {
    @Test
    void defaultIsClosed() {
        AiFeatureGate gate = new AiFeatureGate(new MockEnvironment());
        assertThat(gate.isAvailable()).isFalse();
        assertThatThrownBy(gate::requireAvailable).isInstanceOf(BusinessException.class)
                .hasMessageContaining("暂未开放");
    }

    @Test
    void developmentRequiresExplicitFlag() {
        MockEnvironment disabled = new MockEnvironment();
        disabled.setActiveProfiles("dev");
        MockEnvironment enabled = new MockEnvironment().withProperty("xiaodudou.features.ai-enabled", "true");
        enabled.setActiveProfiles("local");

        assertThat(new AiFeatureGate(disabled).isAvailable()).isFalse();
        assertThat(new AiFeatureGate(enabled).isAvailable()).isTrue();
    }

    @Test
    void productionCannotEnableAiByFlag() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("xiaodudou.features.ai-enabled", "true");
        environment.setActiveProfiles("prod");

        assertThat(new AiFeatureGate(environment).isAvailable()).isFalse();
    }
}
