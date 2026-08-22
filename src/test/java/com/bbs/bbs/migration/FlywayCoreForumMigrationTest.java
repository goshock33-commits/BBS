package com.bbs.bbs.migration;

import com.bbs.bbs.support.MySqlContainerSupport;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest
@TestPropertySource(properties = "spring.flyway.clean-disabled=false")
class FlywayCoreForumMigrationTest extends MySqlContainerSupport {

    @Autowired
    DataSource dataSource;

    @Autowired
    Flyway flyway;

    @BeforeEach
    void cleanDatabase() {
        flyway.clean();
    }

    @Test
    void migratesEmptyDatabaseToCoreForumSchema() throws Exception {
        flyway.migrate();

        try (Connection connection = dataSource.getConnection()) {
            assertCoreForumStructures(connection);
            assertTrue(migrationApplied(connection, "2"));
        }
    }

    @Test
    void preservesAndMapsExistingV1Data() throws Exception {
        migrateOnlyV1();
        insertV1Fixture();

        flyway.migrate();

        try (Connection connection = dataSource.getConnection()) {
            assertCoreForumStructures(connection);
            assertEquals("NORMAL", stringValue(connection,
                    "SELECT content_status FROM tb_bbs_post WHERE post_id = 101"));
            assertEquals("HIDDEN", stringValue(connection,
                    "SELECT content_status FROM tb_bbs_post WHERE post_id = 102"));
            assertEquals("DELETED", stringValue(connection,
                    "SELECT content_status FROM tb_bbs_post WHERE post_id = 103"));
            assertEquals(7, intValue(connection,
                    "SELECT version FROM tb_bbs_post WHERE post_id = 101"));
            assertEquals(0, intValue(connection,
                    "SELECT post_views FROM tb_bbs_post WHERE post_id = 104"));
            assertEquals("NORMAL", stringValue(connection,
                    "SELECT content_status FROM tb_bbs_post_comment WHERE comment_id = 201"));
            assertEquals("HIDDEN", stringValue(connection,
                    "SELECT content_status FROM tb_bbs_post_comment WHERE comment_id = 202"));
            assertEquals("DELETED", stringValue(connection,
                    "SELECT content_status FROM tb_bbs_post_comment WHERE comment_id = 203"));
            assertEquals(1, intValue(connection,
                    "SELECT COUNT(*) FROM tb_bbs_post_collect WHERE user_id = 1 AND post_id = 101"));

            assertThrows(SQLException.class, () -> execute(connection,
                    "INSERT INTO tb_bbs_post_collect(user_id, post_id) VALUES (1, 101)"));
            assertThrows(SQLException.class, () -> execute(connection,
                    "UPDATE tb_bbs_post SET post_comments = -1 WHERE post_id = 101"));
        }
    }

    private void migrateOnlyV1() {
        Flyway.configure()
                .dataSource(dataSource)
                .target(MigrationVersion.fromVersion("1"))
                .cleanDisabled(false)
                .load()
                .migrate();
    }

    private void insertV1Fixture() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO tb_bbs_post(
                        post_id, publish_user_id, post_title, post_content,
                        post_category_id, post_category_name, post_status,
                        post_views, post_comments, post_collects, version, is_deleted)
                    VALUES
                        (101, 1, 'normal', 'body', 1, '提问', 1, 3, 2, 1, 7, 0),
                        (102, 1, 'hidden', 'body', 1, '提问', 0, 0, 0, 0, 0, 0),
                        (103, 1, 'deleted', 'body', 1, '提问', 1, 0, 0, 0, 0, 1),
                        (104, 1, 'negative', 'body', 1, '提问', 1, -3, -2, -1, 0, 0)
                    """);
            statement.executeUpdate("""
                    INSERT INTO tb_bbs_post_comment(
                        comment_id, post_id, comment_user_id, comment_body,
                        comment_status, is_deleted)
                    VALUES
                        (201, 101, 1, 'normal', 1, 0),
                        (202, 101, 1, 'hidden', 0, 0),
                        (203, 101, 1, 'deleted', 1, 1)
                    """);
            statement.executeUpdate("""
                    INSERT INTO tb_bbs_post_collect(record_id, user_id, post_id)
                    VALUES (301, 1, 101)
                    """);
        }
    }

    private void assertCoreForumStructures(Connection connection) throws SQLException {
        assertTrue(columnExists(connection, "tb_bbs_post", "content_status"));
        assertTrue(columnExists(connection, "tb_bbs_post", "version"));
        assertTrue(columnExists(connection, "tb_bbs_post_comment", "content_status"));
        assertTrue(columnExists(connection, "tb_bbs_post_comment", "version"));
        assertTrue(columnExists(connection, "tb_bbs_post_category", "enabled"));
        assertTrue(indexExists(connection, "tb_bbs_post_collect", "uk_collect_user_post"));
        assertTrue(tableExists(connection, "moderation_record"));
        assertTrue(checkConstraintExists(connection, "tb_bbs_post", "chk_post_counters_non_negative"));
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (ResultSet result = connection.getMetaData()
                .getTables(connection.getCatalog(), null, tableName, new String[]{"TABLE"})) {
            return result.next();
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName)
            throws SQLException {
        try (ResultSet result = connection.getMetaData()
                .getColumns(connection.getCatalog(), null, tableName, columnName)) {
            return result.next();
        }
    }

    private boolean indexExists(Connection connection, String tableName, String indexName)
            throws SQLException {
        try (ResultSet result = connection.getMetaData()
                .getIndexInfo(connection.getCatalog(), null, tableName, true, false)) {
            while (result.next()) {
                if (indexName.equalsIgnoreCase(result.getString("INDEX_NAME"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean checkConstraintExists(Connection connection, String tableName,
                                          String constraintName) throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE constraint_schema = DATABASE()
                  AND table_name = ?
                  AND constraint_name = ?
                  AND constraint_type = 'CHECK'
                """)) {
            statement.setString(1, tableName);
            statement.setString(2, constraintName);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1) == 1;
            }
        }
    }

    private boolean migrationApplied(Connection connection, String version) throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE success = 1 AND version = ?
                """)) {
            statement.setString(1, version);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getInt(1) == 1;
            }
        }
    }

    private String stringValue(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getString(1);
        }
    }

    private int intValue(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}
