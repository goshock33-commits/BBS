package com.bbs.bbs.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreForumMigrationScriptTest {

    private static final String MIGRATION =
            "db/migration/V2__core_forum_state_and_moderation.sql";

    @Test
    void definesPhaseTwoSchemaContracts() throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(MIGRATION)) {
            assertNotNull(stream, "Phase 2 Flyway migration must exist");
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                    .toLowerCase();

            assertContains(sql, "content_status");
            assertContains(sql, "version");
            assertContains(sql, "enabled");
            assertContains(sql, "uk_collect_user_post");
            assertContains(sql, "chk_post_counters_non_negative");
            assertContains(sql, "moderation_record");
            assertContains(sql, "operator_user_id");
            assertContains(sql, "reason");
        }
    }

    private void assertContains(String sql, String requiredToken) {
        assertTrue(sql.contains(requiredToken),
                () -> "Migration must define " + requiredToken);
    }
}
