package com.stocksync.common.numbering;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class DocumentNumberServiceTest {
    private final DocumentNumberService service=new DocumentNumberService(new JdbcTemplate());
    @Test void financialYearStartsInApril(){
        assertThat(service.financialYear(LocalDate.of(2026,4,1))).isEqualTo("2026-27");
        assertThat(service.financialYear(LocalDate.of(2027,3,31))).isEqualTo("2026-27");
        assertThat(service.financialYear(LocalDate.of(2027,4,1))).isEqualTo("2027-28");
    }
}
