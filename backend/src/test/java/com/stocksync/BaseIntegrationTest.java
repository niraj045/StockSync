package com.stocksync;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    static final MySQLContainer<?> mysql;

    static {
        MySQLContainer<?> container = null;
        try {
            container = new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("shuttering_inventory_test")
                    .withUsername("inventory_test")
                    .withPassword("inventory_test_password");
            container.start();
            System.out.println("Testcontainers MySQL started successfully.");
        } catch (Exception e) {
            System.out.println("Docker is not available. Falling back to local host MySQL: " + e.getMessage());
            container = null;
        }
        mysql = container;
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (mysql != null && mysql.isRunning()) {
            registry.add("spring.datasource.url", mysql::getJdbcUrl);
            registry.add("spring.datasource.username", mysql::getUsername);
            registry.add("spring.datasource.password", mysql::getPassword);
        }
    }
}
