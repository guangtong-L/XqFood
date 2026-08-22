package ai.xiaodudou.module.ai.service;

import ai.xiaodudou.module.ai.mapper.AiCallLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** AI 日志每日物理清理；生产环境由启动闸门强制要求 1..365 天。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiLogRetentionService {

    private final AiCallLogMapper mapper;

    @Value("${xiaodudou.ai.log-retention-days:0}")
    private int retentionDays;

    @Scheduled(cron = "${xiaodudou.ai.log-cleanup-cron:0 30 3 * * *}")
    public void scheduledCleanup() {
        int deleted = cleanupNow(LocalDateTime.now());
        if (deleted > 0) log.info("AI 最小化日志清理完成，删除数量={}", deleted);
    }

    public int cleanupNow(LocalDateTime now) {
        if (retentionDays < 1 || retentionDays > 365) return 0;
        return mapper.deleteCreatedBefore(now.minusDays(retentionDays));
    }

    void setRetentionDaysForTest(int retentionDays) {
        this.retentionDays = retentionDays;
    }
}
