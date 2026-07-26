package com.stocksync.common.numbering;

import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentNumberService {
    private final JdbcTemplate jdbc;

    public DocumentNumberService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public String next(DocumentType type, LocalDate documentDate) {
        String financialYear = financialYear(documentDate);
        jdbc.update("""
                INSERT INTO document_number_sequences
                    (document_type, financial_year, prefix, last_number, version, updated_at)
                VALUES (?, ?, ?, LAST_INSERT_ID(1), 0, CURRENT_TIMESTAMP(6))
                ON DUPLICATE KEY UPDATE
                    last_number = LAST_INSERT_ID(last_number + 1),
                    version = version + 1,
                    updated_at = CURRENT_TIMESTAMP(6)
                """, type.name(), financialYear, type.prefix());
        Long next = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return "%s/%s/%04d".formatted(type.prefix(), financialYear, next);
    }

    public String financialYear(LocalDate date) {
        int start = date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
        return "%d-%02d".formatted(start, (start + 1) % 100);
    }
}
