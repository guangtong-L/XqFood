package ai.xiaodudou.module.nutrition;

import ai.xiaodudou.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class NutritionControllerSafetyTest {

    @Test
    void reportIsClosedByDefault() {
        assertThatThrownBy(() -> new NutritionController(mock(NutritionEstimationService.class), false).report())
                .isInstanceOf(BusinessException.class).hasMessageContaining("暂未开放");
    }

    @Test
    void accidentalFlagDoesNotRestoreUnverifiedTargetModel() {
        assertThatThrownBy(() -> new NutritionController(mock(NutritionEstimationService.class), true).report())
                .isInstanceOf(BusinessException.class).hasMessageContaining("尚未通过验收");
    }
}
