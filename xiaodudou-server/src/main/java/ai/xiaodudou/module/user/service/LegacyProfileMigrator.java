package ai.xiaodudou.module.user.service;

import ai.xiaodudou.module.user.entity.UserProfile;
import ai.xiaodudou.module.user.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 分批、幂等迁移历史明文画像；任一条失败时异常上抛。 */
@Service
@RequiredArgsConstructor
public class LegacyProfileMigrator {

    private static final int BATCH_SIZE = 100;
    private final UserProfileMapper profileMapper;
    private final ProfileService profileService;

    public int migrateAll() {
        int migrated = 0;
        while (true) {
            List<UserProfile> batch = profileMapper.selectLegacyBatch(BATCH_SIZE);
            if (batch.isEmpty()) return migrated;
            for (UserProfile profile : batch) {
                profileService.migrateLegacy(profile);
                migrated++;
            }
        }
    }
}
