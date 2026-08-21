package com.bbs.bbs.migration;

import com.bbs.bbs.support.MySqlContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest
class PostViewMigrationTest extends MySqlContainerSupport {

    @Autowired
    DataSource dataSource;

    @Test
    void v3CreatesUniquePostAndUserViewRecords() throws Exception {
        try (var connection = dataSource.getConnection()) {
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("""
                         SELECT version
                         FROM flyway_schema_history
                         WHERE success = 1
                         ORDER BY installed_rank DESC
                         LIMIT 1
                         """)) {
                assertTrue(result.next());
                assertEquals("3", result.getString(1));
            }

            List<String> uniqueColumns = new ArrayList<>();
            try (var indexes = connection.getMetaData().getIndexInfo(
                    connection.getCatalog(), null, "tb_bbs_post_view", true, false)) {
                while (indexes.next()) {
                    if ("uk_post_view_post_user".equalsIgnoreCase(
                            indexes.getString("INDEX_NAME"))) {
                        uniqueColumns.add(indexes.getString("COLUMN_NAME"));
                    }
                }
            }
            assertEquals(List.of("post_id", "user_id"), uniqueColumns);

            try (var statement = connection.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS = 0");
                statement.executeUpdate("""
                        INSERT INTO tb_bbs_post_view (post_id, user_id)
                        VALUES (900000001, 900000002)
                        """);
                assertThrows(SQLException.class, () -> statement.executeUpdate("""
                        INSERT INTO tb_bbs_post_view (post_id, user_id)
                        VALUES (900000001, 900000002)
                        """));
                statement.executeUpdate("""
                        DELETE FROM tb_bbs_post_view
                        WHERE post_id = 900000001 AND user_id = 900000002
                        """);
                statement.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
        }
    }
}
