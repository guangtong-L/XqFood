package ai.xiaodudou.module.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/** 生产启动时强制迁移历史画像；失败将中止应用启动。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LegacyProfileMigrationRunner implements ApplicationRunner {

    private final Environment environment;
    private final ProfileEncryptionService encryptionService;
    private final LegacyProfileMigrator migrator;

    @Override
    public void run(ApplicationArguments args) {
        boolean production = Arrays.stream(environment.getActiveProfiles())
                .anyMatch("prod"::equalsIgnoreCase);
        if (!production) return;
        if (!encryptionService.isReady()) {
            throw new IllegalStateException("生产环境缺少敏感画像加密密钥");
        }
        int migrated = migrator.migrateAll();
        log.info("历史敏感画像迁移完成，迁移数量={}", migrated);
    }
}
