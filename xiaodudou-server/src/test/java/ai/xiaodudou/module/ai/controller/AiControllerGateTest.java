package ai.xiaodudou.module.ai.controller;

import ai.xiaodudou.common.exception.BusinessException;
import ai.xiaodudou.common.result.ResultCode;
import ai.xiaodudou.config.RuntimeModePolicy;
import ai.xiaodudou.module.ai.client.ZhipuClient;
import ai.xiaodudou.module.ai.dto.RecommendRequest;
import ai.xiaodudou.module.ai.filter.AllergyFilter;
import ai.xiaodudou.module.ai.limiter.AiRateLimiter;
import ai.xiaodudou.module.ai.service.*;
import ai.xiaodudou.module.recipe.mapper.RecipeMapper;
import ai.xiaodudou.module.user.service.ProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AiControllerGateTest {
    private final ZhipuClient zhipu = mock(ZhipuClient.class);
    private final AiRateLimiter limiter = mock(AiRateLimiter.class);
    private final AiController controller = new AiController(
            mock(RecipeMapper.class), mock(ProfileService.class), zhipu, limiter,
            mock(AllergyFilter.class), mock(AiAuditLogService.class), mock(RuntimeModePolicy.class),
            new AiFeatureGate(new MockEnvironment().withProperty("xiaodudou.features.ai-enabled", "true")),
            new ImageUploadValidator(), new AiOutputParser(new ObjectMapper()),
            new AiPromptBuilder(new ObjectMapper()));

    @Test
    void everyAiEndpointRejectsBeforeQuotaUploadOrProviderWhenGateClosed() {
        assertClosed(controller::quota);
        assertClosed(() -> controller.recognize(new MockMultipartFile("image", new byte[0])));
        assertClosed(() -> controller.recommend(new RecommendRequest()));
        verifyNoInteractions(limiter, zhipu);
    }

    private void assertClosed(Runnable call) {
        BusinessException error = assertThrows(BusinessException.class, call::run);
        assertEquals(ResultCode.FEATURE_NOT_AVAILABLE.getCode(), error.getCode());
    }
}
