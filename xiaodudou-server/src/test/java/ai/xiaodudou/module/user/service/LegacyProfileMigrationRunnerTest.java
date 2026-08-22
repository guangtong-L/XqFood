package ai.xiaodudou.module.user.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegacyProfileMigrationRunnerTest {

    @Test
    void productionMigrationFailureMustRejectStartup() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        ProfileEncryptionService encryption = mock(ProfileEncryptionService.class);
        LegacyProfileMigrator migrator = mock(LegacyProfileMigrator.class);
        when(encryption.isReady()).thenReturn(true);
        when(migrator.migrateAll()).thenThrow(new IllegalStateException("legacy row failed"));

        assertThatThrownBy(() -> new LegacyProfileMigrationRunner(environment, encryption, migrator)
                .run(mock(ApplicationArguments.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("legacy row failed");
    }

    @Test
    void nonProductionDoesNotMutateLegacyRowsDuringStartup() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        ProfileEncryptionService encryption = mock(ProfileEncryptionService.class);
        LegacyProfileMigrator migrator = mock(LegacyProfileMigrator.class);

        new LegacyProfileMigrationRunner(environment, encryption, migrator)
                .run(mock(ApplicationArguments.class));

        verify(migrator, never()).migrateAll();
    }
}
