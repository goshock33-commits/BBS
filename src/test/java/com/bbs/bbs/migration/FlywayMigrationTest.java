package com.bbs.bbs.migration;

import com.bbs.bbs.support.MySqlContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest
class FlywayMigrationTest extends MySqlContainerSupport {

    @Autowired
    DataSource dataSource;

    @Test
    void createsCoreTables() throws Exception {
        try (var connection = dataSource.getConnection();
             ResultSet result = connection.getMetaData()
                     .getTables(null, null, "tb_bbs_user", null)) {
            assertTrue(result.next());
        }
    }
}
