package com.stocksync.common.config;

import java.sql.Connection;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
@Profile({"e2e", "local-scale"})
public class E2eDatabaseGuard implements ApplicationRunner, Ordered {
    private final DataSource dataSource;
    private final String expectedDatabase;

    public E2eDatabaseGuard(
            DataSource dataSource,
            @Value("${stocksync.e2e.expected-database:shuttering_inventory_e2e}") String expectedDatabase) {
        this.dataSource = dataSource;
        this.expectedDatabase = expectedDatabase;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String actualDatabase = connection.getCatalog();
            if (!expectedDatabase.equals(actualDatabase)) {
                throw new IllegalStateException(
                        "E2E safety check failed: expected database '" + expectedDatabase
                                + "' but connected to '" + actualDatabase + "'");
            }
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
