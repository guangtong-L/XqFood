package ai.xiaodudou.module.ai.service;

import ai.xiaodudou.module.ai.entity.AiCallLog;
import ai.xiaodudou.module.ai.mapper.AiCallLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/** 只记录非敏感运行元数据，不接收或保存原始请求、画像、食材名称及完整输出。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAuditLogService {

    private final AiCallLogMapper mapper;

    public void record(Long userId, String endpoint, Integer inputCount, Integer outputCount,
                       List<Long> recipeIds, String modelVersion, int costMs, Integer status) {
        try {
            AiCallLog entry = new AiCallLog();
            entry.setUserId(userId);
            entry.setEndpoint(limit(endpoint, 32));
            entry.setInputCount(nonNegative(inputCount));
            entry.setOutputCount(nonNegative(outputCount));
            entry.setRecipeIds(recipeIds == null ? null : recipeIds.stream().filter(java.util.Objects::nonNull).distinct().limit(20).toList());
            entry.setModelVersion(limit(modelVersion, 64));
            entry.setCostMs(Math.max(0, costMs));
            entry.setAuditStatus(null);
            entry.setStatus(Integer.valueOf(1).equals(status) ? 1 : 0);
            entry.setCreatedAt(LocalDateTime.now());
            mapper.insert(entry);
        } catch (Exception e) {
            log.error("[AiCallLog] 最小化日志写入失败 userId={} endpoint={}", userId, endpoint);
        }
    }

    private Integer nonNegative(Integer value) {
        return value == null ? null : Math.max(0, value);
    }

    private String limit(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
