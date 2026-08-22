package ai.xiaodudou.migration;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class V7MigrationContractTest {
    private static final Path V7 = Path.of("src/main/resources/db/migration/V7__action_idempotency_and_meal_facts.sql");

    @Test
    void preservesLegacyCookAndBuildsDatabaseUniqueness() throws Exception {
        String sql = Files.readString(V7);
        assertThat(sql).contains("ADD COLUMN `action_date` DATE")
                .contains("ADD COLUMN `meal_type` VARCHAR(16)")
                .contains("ADD COLUMN `servings` DECIMAL(5,2)")
                .contains("CONCAT('legacy:', id)")
                .contains("ADD UNIQUE KEY `uk_user_action_idempotency`")
                .contains("(`user_id`, `action`, `idempotency_key`)")
                .doesNotContain("DELETE FROM `t_user_recipe_action` WHERE action = 'cook'");
    }

    @Test
    void favoriteDeduplicationKeepsEarliestAndNewKeysAreDeterministic() throws Exception {
        String sql = Files.readString(V7);
        assertThat(sql).contains("retained.created_at < duplicate_row.created_at")
                .contains("retained.id < duplicate_row.id")
                .contains("CONCAT('favorite:', recipe_id)");
    }
}
