package ai.xiaodudou.module.system;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.SimpleHttpCodeStatusMapper;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.info.BuildProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthControllerTest {

    @SuppressWarnings("unchecked")
    @Test
    void livenessDoesNotDependOnInfrastructureAndReadinessDoes() {
        ApplicationAvailability availability = mock(ApplicationAvailability.class);
        InfrastructureReadinessService readiness = mock(InfrastructureReadinessService.class);
        ObjectProvider<BuildProperties> build = mock(ObjectProvider.class);
        when(availability.getLivenessState()).thenReturn(LivenessState.CORRECT);
        when(readiness.isReady()).thenReturn(false);

        HealthController controller = new HealthController(availability, readiness, build);

        assertThat(controller.health().getStatusCode().value()).isEqualTo(200);
        assertThat(controller.health().getBody().getData()).containsEntry("scope", "liveness");
        assertThat(controller.readiness().getStatusCode().value()).isEqualTo(503);
        assertThat(controller.readiness().getBody().getData()).containsEntry("scope", "readiness");
    }

    @Test
    void actuatorMapsDownToServiceUnavailable() {
        assertThat(new SimpleHttpCodeStatusMapper().getStatusCode(Status.DOWN)).isEqualTo(503);
    }
}
