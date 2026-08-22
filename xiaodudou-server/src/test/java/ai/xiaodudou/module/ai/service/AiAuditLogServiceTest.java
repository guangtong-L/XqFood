package ai.xiaodudou.module.ai.service;

import ai.xiaodudou.module.ai.entity.AiCallLog;
import ai.xiaodudou.module.ai.mapper.AiCallLogMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AiAuditLogServiceTest {

    @Test
    void recordsOnlyMinimalMetadataAndDistinctRecipeIds() {
        AiCallLogMapper mapper = mock(AiCallLogMapper.class);

        new AiAuditLogService(mapper).record(3L, "recommend", 4, 2,
                List.of(10L, 10L, 11L), "model", 120, 1);

        ArgumentCaptor<AiCallLog> captor = ArgumentCaptor.forClass(AiCallLog.class);
        verify(mapper).insert(captor.capture());
        AiCallLog entry = captor.getValue();
        assertThat(entry.getUserId()).isEqualTo(3L);
        assertThat(entry.getInputCount()).isEqualTo(4);
        assertThat(entry.getOutputCount()).isEqualTo(2);
        assertThat(entry.getRecipeIds()).containsExactly(10L, 11L);
        assertThat(AiCallLog.class.getDeclaredFields()).extracting(java.lang.reflect.Field::getName)
                .doesNotContain("inputHash", "inputPayload", "outputPayload");
    }
}
