package ai.xiaodudou.module.community.controller;

import ai.xiaodudou.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommunityControllerSafetyTest {

    @Test
    void communityIsClosedByDefault() {
        assertThatThrownBy(() -> new CommunityController(false).feed())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("暂未开放");
    }

    @Test
    void accidentalFlagCannotRestoreFeedBeforeConsentMechanismExists() {
        assertThatThrownBy(() -> new CommunityController(true).feed())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("同意机制尚未完成");
    }
}
