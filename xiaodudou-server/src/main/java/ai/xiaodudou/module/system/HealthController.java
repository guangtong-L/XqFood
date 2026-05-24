package ai.xiaodudou.module.system;

import ai.xiaodudou.common.result.Result;
import cn.dev33.satoken.annotation.SaIgnore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
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

    @SaIgnore
    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("service", "xiaodudou-server");
        data.put("version", "0.0.1-SNAPSHOT");
        data.put("timestamp", LocalDateTime.now());
        return Result.ok(data);
    }
}
