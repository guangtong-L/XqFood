package ai.xiaodudou.module.user.service;

import ai.xiaodudou.module.user.entity.UserProfile;
import ai.xiaodudou.module.user.mapper.UserProfileMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegacyProfileMigratorTest {

    @Test
    void migratesInBatchesAndStopsWhenNoLegacyRowsRemain() {
        UserProfileMapper mapper = mock(UserProfileMapper.class);
        ProfileService profileService = mock(ProfileService.class);
        UserProfile first = profile(1L);
        UserProfile second = profile(2L);
        when(mapper.selectLegacyBatch(100)).thenReturn(List.of(first, second), List.of());

        int migrated = new LegacyProfileMigrator(mapper, profileService).migrateAll();

        assertThat(migrated).isEqualTo(2);
        verify(profileService).migrateLegacy(first);
        verify(profileService).migrateLegacy(second);
    }

    @Test
    void anyMigrationFailureMustPropagateInsteadOfSkipping() {
        UserProfileMapper mapper = mock(UserProfileMapper.class);
        ProfileService profileService = mock(ProfileService.class);
        UserProfile row = profile(3L);
        when(mapper.selectLegacyBatch(100)).thenReturn(List.of(row));
        doThrow(new IllegalStateException("migration failed")).when(profileService).migrateLegacy(row);

        assertThatThrownBy(() -> new LegacyProfileMigrator(mapper, profileService).migrateAll())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("migration failed");
    }

    private UserProfile profile(Long id) {
        UserProfile profile = new UserProfile();
        profile.setId(id);
        profile.setStageType("PREPARE");
        return profile;
    }
}
