package ai.xiaodudou.module.system;

import ai.xiaodudou.common.result.Result;
import cn.dev33.satoken.annotation.SaIgnore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.info.BuildProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统健康检查接口
 *
 * @author xiaodudou
 */
@Tag(name = "00 - 系统", description = "健康检查 / 版本信息")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HealthController {

    private final ApplicationAvailability availability;
    private final InfrastructureReadinessService readinessService;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    @SaIgnore
    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public ResponseEntity<Result<Map<String, Object>>> health() {
        boolean live = availability.getLivenessState() == LivenessState.CORRECT;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", live ? "UP" : "DOWN");
        data.put("scope", "liveness");
        data.put("service", "xiaodudou-server");
        BuildProperties build = buildPropertiesProvider.getIfAvailable();
        if (build != null) data.put("version", build.getVersion());
        data.put("timestamp", Instant.now());
        return ResponseEntity.status(live ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(Result.ok(data));
    }

    @SaIgnore
    @GetMapping("/readiness")
    @Operation(summary = "就绪检查")
    public ResponseEntity<Result<Map<String, Object>>> readiness() {
        boolean ready = readinessService.isReady();
        Map<String, Object> data = Map.of(
                "status", ready ? "UP" : "DOWN",
                "scope", "readiness",
                "timestamp", Instant.now());
        return ResponseEntity.status(ready ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(Result.ok(data));
    }
}
