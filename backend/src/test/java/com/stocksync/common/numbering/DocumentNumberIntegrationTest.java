package com.stocksync.common.numbering;

import static org.assertj.core.api.Assertions.assertThat;
import com.stocksync.BaseIntegrationTest;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class DocumentNumberIntegrationTest extends BaseIntegrationTest {
    @Autowired DocumentNumberService service;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach void reset(){jdbc.update("DELETE FROM document_number_sequences WHERE document_type IN ('QUOTATION','AGREEMENT')");}

    @Test void concurrentAllocationIsUniqueAndGapFree() throws Exception {
        int count=16;ExecutorService pool=Executors.newFixedThreadPool(8);
        try {
            List<Callable<String>> tasks=new ArrayList<>();
            for(int i=0;i<count;i++)tasks.add(()->service.next(DocumentType.QUOTATION,LocalDate.of(2026,7,26)));
            Set<String> numbers=new HashSet<>();
            for(Future<String> future:pool.invokeAll(tasks))numbers.add(future.get());
            assertThat(numbers).hasSize(count).contains("QT/2026-27/0001","QT/2026-27/0016");
        } finally { pool.shutdownNow(); }
    }

    @Test void committedNumbersAreNotReusedAndYearsAreIndependent(){
        assertThat(service.next(DocumentType.QUOTATION,LocalDate.of(2026,7,26))).isEqualTo("QT/2026-27/0001");
        assertThat(service.next(DocumentType.QUOTATION,LocalDate.of(2026,7,26))).isEqualTo("QT/2026-27/0002");
        assertThat(service.next(DocumentType.QUOTATION,LocalDate.of(2027,4,1))).isEqualTo("QT/2027-28/0001");
    }

    @Test void agreementSequenceIsIndependentAndRollsAtFinancialYear(){
        assertThat(service.next(DocumentType.QUOTATION,LocalDate.of(2026,7,26))).isEqualTo("QT/2026-27/0001");
        assertThat(service.next(DocumentType.AGREEMENT,LocalDate.of(2026,7,26))).isEqualTo("AGR/2026-27/0001");
        assertThat(service.next(DocumentType.AGREEMENT,LocalDate.of(2026,7,27))).isEqualTo("AGR/2026-27/0002");
        assertThat(service.next(DocumentType.AGREEMENT,LocalDate.of(2027,4,1))).isEqualTo("AGR/2027-28/0001");
    }

    @Test void concurrentAgreementAllocationIsUnique() throws Exception {
        int count=12;ExecutorService pool=Executors.newFixedThreadPool(6);
        try {
            List<Callable<String>> tasks=new ArrayList<>();
            for(int i=0;i<count;i++)tasks.add(()->service.next(DocumentType.AGREEMENT,LocalDate.of(2026,7,26)));
            Set<String> numbers=new HashSet<>();
            for(Future<String> future:pool.invokeAll(tasks))numbers.add(future.get());
            assertThat(numbers).hasSize(count).contains("AGR/2026-27/0001","AGR/2026-27/0012");
        } finally { pool.shutdownNow(); }
    }
}
