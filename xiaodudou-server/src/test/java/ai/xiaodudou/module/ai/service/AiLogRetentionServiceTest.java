package ai.xiaodudou.module.ai.service;

import ai.xiaodudou.module.ai.mapper.AiCallLogMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AiLogRetentionServiceTest {

    @Test
    void cleanupDeletesRowsOlderThanConfiguredRetention() {
        AiCallLogMapper mapper = mock(AiCallLogMapper.class);
        AiLogRetentionService service = new AiLogRetentionService(mapper);
        service.setRetentionDaysForTest(30);
        LocalDateTime now = LocalDateTime.of(2026, 8, 21, 3, 30);
        when(mapper.deleteCreatedBefore(now.minusDays(30))).thenReturn(5);

        assertThat(service.cleanupNow(now)).isEqualTo(5);
        verify(mapper).deleteCreatedBefore(now.minusDays(30));
    }

    @Test
    void invalidNonProductionRetentionDoesNotDeleteAnything() {
        AiCallLogMapper mapper = mock(AiCallLogMapper.class);
        AiLogRetentionService service = new AiLogRetentionService(mapper);
        service.setRetentionDaysForTest(0);

        assertThat(service.cleanupNow(LocalDateTime.now())).isZero();
        verifyNoInteractions(mapper);
    }
}
