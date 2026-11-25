package com.recipehub;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class BackendApplicationTests {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void flywayCreatedSessionTables() {
        Integer n = jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'spring_session'", Integer.class);
        assertThat(n).isEqualTo(1);
    }
}
