package com.stocksync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StockSyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockSyncApplication.class, args);
    }
}
