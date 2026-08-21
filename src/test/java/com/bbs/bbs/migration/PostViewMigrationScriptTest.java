package com.bbs.bbs.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostViewMigrationScriptTest {

    @Test
    void v3CreatesUniqueAuthenticatedPostViews() throws IOException {
        try (InputStream input = getClass().getResourceAsStream(
                "/db/migration/V3__authenticated_post_views.sql")) {
            assertNotNull(input, "V3 post-view migration must exist");
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .toLowerCase();

            assertTrue(sql.contains("create table tb_bbs_post_view"));
            assertTrue(sql.contains("post_id bigint not null"));
            assertTrue(sql.contains("user_id bigint not null"));
            assertTrue(sql.contains(
                    "unique key uk_post_view_post_user (post_id, user_id)"));
        }
    }
}
