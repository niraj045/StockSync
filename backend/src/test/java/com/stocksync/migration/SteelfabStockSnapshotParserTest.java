package com.stocksync.migration;

import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.migration.entity.ImportLocationType;
import com.stocksync.migration.parser.SteelfabStockSnapshotParser;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SteelfabStockSnapshotParserTest {
    private final SteelfabStockSnapshotParser parser = new SteelfabStockSnapshotParser();

    @Test
    void parsesTheControlledClientSnapshotAndRecalculatesAuthoritativeTotals() {
        var parsed = parser.parse(workbook());

        assertThat(parsed.sourceItems()).hasSize(42);
        assertThat(parsed.locations()).hasSize(16);
        assertThat(parsed.locations().keySet()).containsExactly(
                "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R");

        var siteRows = parsed.balances().stream()
                .filter(row -> row.locationType() == ImportLocationType.PARTY_OR_SITE)
                .toList();
        var godownRows = parsed.balances().stream()
                .filter(row -> row.locationType() == ImportLocationType.GODOWN)
                .toList();

        assertThat(siteRows).hasSize(120);
        assertThat(godownRows).hasSize(32);
        assertThat(parsed.balances()).hasSize(152);
        assertThat(parsed.balances()).allMatch(row -> row.quantity().signum() > 0);

        assertThat(parsed.partyTotal()).isEqualByComparingTo("20637");
        assertThat(parsed.godownTotal()).isEqualByComparingTo("25382");
        assertThat(parsed.combinedTotal()).isEqualByComparingTo("46019");

        BigDecimal omittedColumns = siteRows.stream()
                .filter(row -> "P".equals(row.sourceExcelColumn())
                        || "Q".equals(row.sourceExcelColumn())
                        || "R".equals(row.sourceExcelColumn()))
                .map(SteelfabStockSnapshotParser.ParsedBalance::quantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(omittedColumns).isEqualByComparingTo("2418");
        assertThat(parsed.partyTotal().subtract(omittedColumns)).isEqualByComparingTo("18219");

        assertThat(parsed.partySnapshotDate()).isEqualTo(LocalDate.of(2026, 7, 25));
        assertThat(parsed.godownSnapshotDate()).isEqualTo(LocalDate.of(2026, 7, 16));
        assertThat(parsed.warnings()).anyMatch(warning -> warning.contains("columns T and V are ignored"));
        assertThat(parsed.warnings()).anyMatch(warning -> warning.contains("while godown stock is dated"));
    }

    @Test
    void preservesDuplicateRowsAndFlagsEveryBalanceForBothDuplicateSourceItems() {
        var parsed = parser.parse(workbook());
        var duplicates = parsed.sourceItems().stream()
                .collect(Collectors.groupingBy(
                        item -> item.sourceItemName().trim().replaceAll("\\s+", " ").toLowerCase(),
                        Collectors.toList()))
                .get("8ft & 10ft plate pipe");

        assertThat(duplicates).extracting(SteelfabStockSnapshotParser.SourceItem::sourceSrNumber)
                .containsExactly("33", "35");
        assertThat(duplicates).extracting(SteelfabStockSnapshotParser.SourceItem::sourceExcelRow)
                .doesNotHaveDuplicates();
        assertThat(parsed.balances().stream()
                .filter(row -> "33".equals(row.sourceSrNumber()) || "35".equals(row.sourceSrNumber())))
                .allMatch(row -> row.warning() != null && row.warning().contains("Duplicate source item name"));
    }

    @Test
    void readsAllNamedLocationColumnsIncludingTheThreeOmittedByTheSourceFormula() {
        var parsed = parser.parse(workbook());
        Map<String, BigDecimal> totals = parsed.balances().stream()
                .filter(row -> row.locationType() == ImportLocationType.PARTY_OR_SITE)
                .collect(Collectors.groupingBy(
                        SteelfabStockSnapshotParser.ParsedBalance::sourceLocationName,
                        Collectors.reducing(BigDecimal.ZERO,
                                SteelfabStockSnapshotParser.ParsedBalance::quantity,
                                BigDecimal::add)));

        assertThat(totals).hasSize(16);
        assertThat(totals.get("ANV")).isEqualByComparingTo("700");
        assertThat(totals.get("Rocks & Logs")).isEqualByComparingTo("318");
        assertThat(totals.get("Innovator Façade")).isEqualByComparingTo("1400");
    }

    @Test
    void rejectsNegativeQuantities() throws Exception {
        byte[] modified;
        try (InputStream source = workbook();
             var workbook = WorkbookFactory.create(source);
             var output = new ByteArrayOutputStream()) {
            workbook.getSheet("Sheet1").getRow(4).getCell(2).setCellValue(-1);
            workbook.write(output);
            modified = output.toByteArray();
        }

        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(modified)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Negative quantity at C5");
    }

    private InputStream workbook() {
        InputStream input = getClass().getResourceAsStream("/client-data/Stock of material as on 25-07-26.xlsx");
        if (input == null) throw new IllegalStateException("Client workbook test fixture is missing");
        return input;
    }
}
