package ai.xiaodudou.module.system;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/** Actuator readiness 复用公开就绪检查的同一真实探测逻辑。 */
@Component("infrastructureReadiness")
@RequiredArgsConstructor
public class InfrastructureReadinessHealthIndicator implements HealthIndicator {

    private final InfrastructureReadinessService readinessService;

    @Override
    public Health health() {
        return readinessService.isReady() ? Health.up().build() : Health.down().build();
    }
}
