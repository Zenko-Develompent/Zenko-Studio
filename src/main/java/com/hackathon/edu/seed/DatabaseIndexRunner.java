package com.hackathon.edu.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseIndexRunner implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        createIndexIfNeeded(
                "idx_user_username_lower",
                "create index if not exists idx_user_username_lower on public.\"user\" (lower(username))"
        );
    }

    private void createIndexIfNeeded(String indexName, String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ex) {
            log.warn("Failed to create index '{}': {}", indexName, ex.getMessage());
        }
    }
}
