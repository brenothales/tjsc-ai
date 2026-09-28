package br.jus.tjsc.ai.process.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteOpenMode;

@Configuration
class ReadOnlyDataSourceConfig {

    @Bean("readOnlyJdbcTemplate")
    JdbcTemplate readOnlyJdbcTemplate(
            @Value("${spring.datasource.url}") String jdbcUrl) {

        String filePath = jdbcUrl.replace("jdbc:sqlite:", "");

        SQLiteConfig config = new SQLiteConfig();
        config.setOpenMode(SQLiteOpenMode.READONLY);
        config.setReadOnly(true);

        SQLiteDataSource dataSource = new SQLiteDataSource(config);
        dataSource.setUrl("jdbc:sqlite:" + filePath);

        return new JdbcTemplate(dataSource);
    }
}
