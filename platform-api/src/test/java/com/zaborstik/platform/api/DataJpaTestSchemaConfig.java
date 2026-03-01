package com.zaborstik.platform.api;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * For DataJpaTest slices: creates H2 schemas (zbrtstk, system) before any JPA use,
 * since Flyway is disabled and Hibernate ddl-auto does not create schemas.
 */
@TestConfiguration
public class DataJpaTestSchemaConfig {

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) throws Exception {
        DataSource ds = properties.initializeDataSourceBuilder().build();
        try (Connection c = ds.getConnection()) {
            if (c == null) throw new IllegalStateException("DataSource.getConnection() returned null");
            ScriptUtils.executeSqlScript(c, new ClassPathResource("db/h2-schemas.sql"));
        }
        return ds;
    }
}
