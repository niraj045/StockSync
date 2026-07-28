package com.stocksync.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksync.BaseIntegrationTest;
import com.stocksync.inventory.entity.Item;
import com.stocksync.inventory.entity.ItemCategory;
import com.stocksync.inventory.repository.ItemCategoryRepository;
import com.stocksync.inventory.repository.ItemRepository;
import com.stocksync.migration.entity.ImportLocationType;
import com.stocksync.migration.entity.ImportValidationStatus;
import com.stocksync.migration.entity.ItemAlias;
import com.stocksync.migration.entity.StockImportRow;
import com.stocksync.migration.parser.SteelfabStockSnapshotParser;
import com.stocksync.migration.repository.ItemAliasRepository;
import com.stocksync.migration.repository.StockImportRowRepository;
import com.stocksync.party.entity.Party;
import com.stocksync.party.repository.PartyRepository;
import com.stocksync.site.entity.Site;
import com.stocksync.site.entity.SiteStatus;
import com.stocksync.site.repository.SiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WithMockUser(username = "operations", roles = "OPERATIONS")
class StockImportIntegrationTest extends BaseIntegrationTest {
    private static final Path STORAGE = Path.of("target", "test-stock-import-storage").toAbsolutePath();

    @DynamicPropertySource
    static void stockImportProperties(DynamicPropertyRegistry registry) {
        registry.add("stocksync.file-storage-path", () -> STORAGE.toString());
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired ItemCategoryRepository categories;
    @Autowired ItemRepository items;
    @Autowired PartyRepository parties;
    @Autowired SiteRepository sites;
    @Autowired ItemAliasRepository aliases;
    @Autowired StockImportRowRepository importRows;

    private final SteelfabStockSnapshotParser parser = new SteelfabStockSnapshotParser();
    private final Map<String, Item> itemBySr = new HashMap<>();
    private Party party;
    private Site site;

    @BeforeEach
    void setup() {
        cleanDatabase();
        ItemCategory category = new ItemCategory();
        category.setName("SCAFFOLDING");
        category.setDescription("Opening stock integration test category");
        category.setActive(true);
        category.setCreatedBy("test");
        category.setUpdatedBy("test");
        category = categories.save(category);

        var source = parser.parse(workbook()).sourceItems();
        for (var sourceItem : source) {
            Item item = new Item();
            item.setItemCode("MAT-" + String.format("%03d", Integer.parseInt(sourceItem.sourceSrNumber())));
            item.setItemName("1".equals(sourceItem.sourceSrNumber()) ? "Canonical alias item" : sourceItem.sourceItemName());
            item.setCategory(category);
            item.setUnit("PIECE");
            item.setMinimumStock(BigDecimal.ZERO);
            item.setActive(true);
            item.setCreatedBy("test");
            item.setUpdatedBy("test");
            item = items.save(item);
            itemBySr.put(sourceItem.sourceSrNumber(), item);
        }

        ItemAlias alias = new ItemAlias();
        alias.setItem(itemBySr.get("1"));
        alias.setAlias(source.getFirst().sourceItemName());
        alias.setSourceSystem(SteelfabStockSnapshotParser.SOURCE_FORMAT);
        aliases.save(alias);

        party = new Party();
        party.setLegalName("Legacy Opening Stock Party");
        party.setActive(true);
        party.setCreatedBy("test");
        party.setUpdatedBy("test");
        party = parties.save(party);

        site = new Site();
        site.setParty(party);
        site.setSiteCode("LEGACY-SITE");
        site.setSiteName("Legacy Opening Stock Site");
        site.setStatus(SiteStatus.ACTIVE);
        site.setCreatedBy("test");
        site.setUpdatedBy("test");
        site = sites.save(site);
    }

    @AfterEach
    void cleanup() {
        cleanDatabase();
    }

    @Test
    void uploadUsesAliasAndExactMappingsWhileAmbiguousDuplicatesRemainUnresolved() throws Exception {
        JsonNode uploaded = uploadWorkbook();
        long batchId = uploaded.get("id").asLong();

        assertThat(uploaded.get("totalSourceRows").asInt()).isEqualTo(42);
        assertThat(uploaded.get("totalBalanceRows").asInt()).isEqualTo(152);
        assertThat(uploaded.get("expectedPartyTotal").decimalValue()).isEqualByComparingTo("20637");
        assertThat(uploaded.get("expectedGodownTotal").decimalValue()).isEqualByComparingTo("25382");
        assertThat(uploaded.get("expectedCombinedTotal").decimalValue()).isEqualByComparingTo("46019");

        List<StockImportRow> rows = importRows.findByBatchIdOrderBySourceExcelRowAscSourceExcelColumnAsc(batchId);
        assertThat(rows.stream().filter(row -> "1".equals(row.getSourceSrNumber())))
                .allMatch(row -> row.getMappedItem() != null && row.getMappedItem().getId().equals(itemBySr.get("1").getId()));
        assertThat(rows.stream().filter(row -> "4".equals(row.getSourceSrNumber())))
                .allMatch(row -> row.getMappedItem() != null && row.getMappedItem().getId().equals(itemBySr.get("4").getId()));
        assertThat(rows.stream().filter(row -> "33".equals(row.getSourceSrNumber()) || "35".equals(row.getSourceSrNumber())))
                .allMatch(row -> row.getMappedItem() == null && row.getValidationStatus() == ImportValidationStatus.MAPPING_REQUIRED);

        StockImportRow duplicate = rows.stream().filter(row -> "33".equals(row.getSourceSrNumber())).findFirst().orElseThrow();
        mapItem(batchId, duplicate, itemBySr.get("33").getId(), false);
        assertThat(importRows.findByBatchIdAndSourceExcelRow(batchId, duplicate.getSourceExcelRow()))
                .allMatch(row -> !row.isDuplicateConfirmed() && row.getValidationStatus() == ImportValidationStatus.MAPPING_REQUIRED);

        duplicate = importRows.findById(duplicate.getId()).orElseThrow();
        mapItem(batchId, duplicate, itemBySr.get("33").getId(), true);
        assertThat(importRows.findByBatchIdAndSourceExcelRow(batchId, duplicate.getSourceExcelRow()))
                .allMatch(StockImportRow::isDuplicateConfirmed);

        mvc.perform(post("/api/v1/stock-imports/{id}/validate", batchId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postable").value(false))
                .andExpect(jsonPath("$.errorRows").value(org.hamcrest.Matchers.greaterThan(0)));
        mvc.perform(post("/api/v1/stock-imports/{id}/post", batchId)
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"confirmed":true,"expectedChecksum":"%s"}
                                """.formatted(uploaded.get("fileChecksum").asText())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IMPORT_NOT_VALIDATED"));
    }

    @Test
    void confirmedImportItemCreationPreservesUnknownBusinessValuesAsNull() throws Exception {
        long batchId = uploadWorkbook().get("id").asLong();
        StockImportRow row = importRows.findByBatchIdOrderBySourceExcelRowAscSourceExcelColumnAsc(batchId).stream()
                .filter(candidate -> "4".equals(candidate.getSourceSrNumber()))
                .findFirst().orElseThrow();

        mvc.perform(put("/api/v1/stock-imports/{id}/rows/{rowId}/item-mapping", batchId, row.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"createNew":true,"itemCode":"LEGACY-NEW-004","itemName":"Confirmed Legacy Item",
                                 "saveAlias":true,"confirmDuplicate":false,"exclude":false,"version":%d}
                                """.formatted(row.getVersion())))
                .andExpect(status().isOk());

        Item created = items.findByItemCodeIgnoreCase("LEGACY-NEW-004").orElseThrow();
        assertThat(jdbc.queryForObject("""
                SELECT c.name FROM item_categories c
                JOIN items i ON i.category_id=c.id
                WHERE i.id=?
                """, String.class, created.getId())).isEqualTo("SCAFFOLDING");
        assertThat(created.getUnit()).isEqualTo("PIECE");
        assertThat(created.getWeightPerPiece()).isNull();
        assertThat(created.getPurchaseValue()).isNull();
        assertThat(created.getRentalConfiguration()).isNull();
        assertThat(created.getLossRate()).isNull();
        assertThat(created.getScrapValue()).isNull();
        assertThat(created.getMinimumStock()).isNull();
        assertThat(aliases.existsByAliasIgnoreCaseAndSourceSystem(
                row.getSourceItemName(), SteelfabStockSnapshotParser.SOURCE_FORMAT)).isTrue();
    }

    @Test
    void postsAndReversesAllOpeningBalancesWithoutCreatingOperationalDocuments() throws Exception {
        PreparedBatch prepared = prepareFullyMappedBatch();

        mvc.perform(post("/api/v1/stock-imports/{id}/validate", prepared.id()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postable").value(true))
                .andExpect(jsonPath("$.mappedPartyTotal").value(20637.0))
                .andExpect(jsonPath("$.mappedGodownTotal").value(25382.0))
                .andExpect(jsonPath("$.mappedCombinedTotal").value(46019.0));

        mvc.perform(post("/api/v1/stock-imports/{id}/post", prepared.id())
                        .with(user("operations").roles("OPERATIONS")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody(prepared.checksum())))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/stock-imports/{id}/post", prepared.id())
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody(prepared.checksum())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("POSTED"))
                .andExpect(jsonPath("$.postedRows").value(152))
                .andExpect(jsonPath("$.postedPartyTotal").value(20637.0))
                .andExpect(jsonPath("$.postedGodownTotal").value(25382.0))
                .andExpect(jsonPath("$.postedCombinedTotal").value(46019.0));

        assertThat(decimal("SELECT COALESCE(SUM(available_quantity),0) FROM stock_balances")).isEqualByComparingTo("25382");
        assertThat(decimal("SELECT COALESCE(SUM(issued_quantity),0) FROM stock_balances")).isEqualByComparingTo("20637");
        assertThat(decimal("SELECT COALESCE(SUM(pending_quantity),0) FROM site_stock_balances")).isEqualByComparingTo("20637");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_transactions WHERE source_type='STOCK_IMPORT'", Integer.class)).isEqualTo(152);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_transactions WHERE transaction_type='OPENING_GODOWN_BALANCE'", Integer.class)).isEqualTo(32);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_transactions WHERE transaction_type='OPENING_SITE_BALANCE'", Integer.class)).isEqualTo(120);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM purchases", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM site_orders", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM agreements", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_activity_logs WHERE action='STOCK_IMPORT_POSTED'", Integer.class)).isEqualTo(1);

        mvc.perform(post("/api/v1/stock-imports/{id}/post", prepared.id())
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody(prepared.checksum())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IMPORT_ALREADY_POSTED"));

        mvc.perform(post("/api/v1/stock-imports/{id}/reverse", prepared.id())
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Isolated integration-test reversal\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVERSED"));

        assertThat(decimal("SELECT COALESCE(SUM(available_quantity),0) FROM stock_balances")).isEqualByComparingTo("0");
        assertThat(decimal("SELECT COALESCE(SUM(issued_quantity),0) FROM stock_balances")).isEqualByComparingTo("0");
        assertThat(decimal("SELECT COALESCE(SUM(pending_quantity),0) FROM site_stock_balances")).isEqualByComparingTo("0");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_transactions WHERE source_type='STOCK_IMPORT'", Integer.class)).isEqualTo(152);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_transactions WHERE source_type='STOCK_IMPORT_REVERSAL'", Integer.class)).isEqualTo(152);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_activity_logs WHERE action='STOCK_IMPORT_REVERSED'", Integer.class)).isEqualTo(1);

        mvc.perform(post("/api/v1/stock-imports/{id}/reverse", prepared.id())
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Second reversal\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IMPORT_ALREADY_REVERSED"));
    }

    @Test
    void autoMapCreatesLegacyItemsAndLocationsThenValidatesForPosting() throws Exception {
        JsonNode uploaded = uploadWorkbook();
        long id = uploaded.get("id").asLong();

        mvc.perform(post("/api/v1/stock-imports/{id}/auto-map", id).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDATED"))
                .andExpect(jsonPath("$.postable").value(true))
                .andExpect(jsonPath("$.mappedRows").value(152))
                .andExpect(jsonPath("$.warningRows").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.errorRows").value(0))
                .andExpect(jsonPath("$.mappedPartyTotal").value(20637.0))
                .andExpect(jsonPath("$.mappedGodownTotal").value(25382.0));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM items", Integer.class)).isEqualTo(42);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM parties", Integer.class)).isEqualTo(16);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sites", Integer.class)).isEqualTo(16);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_import_location_mappings", Integer.class)).isEqualTo(16);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_import_rows WHERE mapped_item_id IS NULL", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_import_rows WHERE location_type='PARTY_OR_SITE' AND mapped_site_id IS NULL", Integer.class)).isZero();

        mvc.perform(post("/api/v1/stock-imports/{id}/post", id)
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody(uploaded.get("fileChecksum").asText())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("POSTED"))
                .andExpect(jsonPath("$.postedRows").value(152));

        assertThat(decimal("SELECT COALESCE(SUM(available_quantity),0) FROM stock_balances")).isEqualByComparingTo("25382");
        assertThat(decimal("SELECT COALESCE(SUM(issued_quantity),0) FROM stock_balances")).isEqualByComparingTo("20637");
    }

    @Test
    void postingFailureRollsBackEveryBalanceAndLedgerWrite() throws Exception {
        PreparedBatch prepared = prepareFullyMappedBatch();
        mvc.perform(post("/api/v1/stock-imports/{id}/validate", prepared.id()).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.postable").value(true));

        Item last = itemBySr.get("42");
        last.setActive(false);
        last.setUpdatedBy("test");
        items.saveAndFlush(last);

        mvc.perform(post("/api/v1/stock-imports/{id}/post", prepared.id())
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postBody(prepared.checksum())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ITEM_NOT_FOUND_OR_INACTIVE"));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_transactions WHERE import_batch_id=?", Integer.class, prepared.id())).isZero();
        assertThat(decimal("SELECT COALESCE(SUM(available_quantity + issued_quantity),0) FROM stock_balances")).isEqualByComparingTo("0");
        assertThat(jdbc.queryForObject("SELECT status FROM stock_import_batches WHERE id=?", String.class, prepared.id())).isEqualTo("VALIDATED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_activity_logs WHERE action='STOCK_IMPORT_FAILED'", Integer.class)).isEqualTo(1);
    }

    @Test
    void reversalIsBlockedWhenALaterStockMovementExists() throws Exception {
        PreparedBatch prepared = prepareFullyMappedBatch();
        mvc.perform(post("/api/v1/stock-imports/{id}/validate", prepared.id()).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.postable").value(true));
        mvc.perform(post("/api/v1/stock-imports/{id}/post", prepared.id())
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(postBody(prepared.checksum())))
                .andExpect(status().isOk());

        jdbc.update("""
                INSERT INTO stock_transactions
                    (item_id,transaction_type,transaction_date,quantity,weight,direction,source_type,source_id,notes,created_by,created_at)
                VALUES (?, 'ADJUSTMENT_IN', CURRENT_DATE, 1, 0, 'IN', 'ADJUSTMENT', 999999,
                        'Later movement used to test import reversal protection', 'test', CURRENT_TIMESTAMP(6))
                """, itemBySr.get("1").getId());

        mvc.perform(post("/api/v1/stock-imports/{id}/reverse", prepared.id())
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Must be blocked by later movement\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IMPORT_REVERSAL_UNSAFE"));

        assertThat(jdbc.queryForObject("SELECT status FROM stock_import_batches WHERE id=?", String.class, prepared.id())).isEqualTo("POSTED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_transactions WHERE source_type='STOCK_IMPORT_REVERSAL'", Integer.class)).isZero();
    }

    @Test
    void rolesEnforceReadOnlyAndAdministrativePostingBoundaries() throws Exception {
        MockMultipartFile file = workbookFile();
        mvc.perform(multipart("/api/v1/stock-imports/upload").file(file)
                        .with(user("viewer").roles("VIEWER")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/api/v1/stock-imports/upload").file(workbookFile())
                        .with(user("accounts").roles("ACCOUNTS")).with(csrf()))
                .andExpect(status().isForbidden());

        JsonNode uploaded = uploadWorkbook();
        long id = uploaded.get("id").asLong();
        mvc.perform(get("/api/v1/stock-imports/{id}", id).with(user("viewer").roles("VIEWER")))
                .andExpect(status().isOk());

        StockImportRow row = importRows.findByBatchIdOrderBySourceExcelRowAscSourceExcelColumnAsc(id).getFirst();
        mvc.perform(put("/api/v1/stock-imports/{id}/rows/{rowId}/item-mapping", id, row.getId())
                        .with(user("accounts").roles("ACCOUNTS")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemId":%d,"createNew":false,"saveAlias":false,"confirmDuplicate":false,
                                 "exclude":false,"version":%d}
                                """.formatted(itemBySr.get(row.getSourceSrNumber()).getId(), row.getVersion())))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/stock-imports/{id}/reverse", id)
                        .with(user("operations").roles("OPERATIONS")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Not permitted\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/stock-imports/{id}/reverse", id)
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    private PreparedBatch prepareFullyMappedBatch() throws Exception {
        JsonNode uploaded = uploadWorkbook();
        long id = uploaded.get("id").asLong();

        for (String sr : List.of("2", "3", "33", "35")) {
            List<StockImportRow> sourceRows = importRows.findByBatchIdOrderBySourceExcelRowAscSourceExcelColumnAsc(id)
                    .stream().filter(row -> sr.equals(row.getSourceSrNumber())).toList();
            if (sourceRows.isEmpty()) continue;
            mapItem(id, sourceRows.getFirst(), itemBySr.get(sr).getId(), true);
        }

        List<String> columns = parser.parse(workbook()).locations().keySet().stream().toList();
        for (String column : columns) {
            mvc.perform(put("/api/v1/stock-imports/{id}/location-mappings", id).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"sourceExcelColumn":"%s","partyId":%d,"siteId":%d,
                                     "createParty":false,"createSite":false}
                                    """.formatted(column, party.getId(), site.getId())))
                    .andExpect(status().isOk());
        }
        return new PreparedBatch(id, uploaded.get("fileChecksum").asText());
    }

    private void mapItem(long batchId, StockImportRow row, long itemId, boolean confirm) throws Exception {
        mvc.perform(put("/api/v1/stock-imports/{id}/rows/{rowId}/item-mapping", batchId, row.getId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"itemId":%d,"createNew":false,"saveAlias":false,"confirmDuplicate":%s,
                                 "exclude":false,"version":%d}
                                """.formatted(itemId, confirm, row.getVersion())))
                .andExpect(status().isOk());
    }

    private JsonNode uploadWorkbook() throws Exception {
        String body = mvc.perform(multipart("/api/v1/stock-imports/upload").file(workbookFile())
                        .param("notes", "Controlled client opening-stock integration test").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MAPPING_REQUIRED"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private MockMultipartFile workbookFile() throws IOException {
        try (InputStream input = workbook()) {
            return new MockMultipartFile(
                    "file",
                    "Stock of material as on 25-07-26.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    input.readAllBytes());
        }
    }

    private InputStream workbook() {
        InputStream input = getClass().getResourceAsStream("/client-data/Stock of material as on 25-07-26.xlsx");
        if (input == null) throw new IllegalStateException("Client workbook test fixture is missing");
        return input;
    }

    private String postBody(String checksum) {
        return """
                {"confirmed":true,"expectedChecksum":"%s"}
                """.formatted(checksum);
    }

    private BigDecimal decimal(String sql) {
        return jdbc.queryForObject(sql, BigDecimal.class);
    }

    private void cleanDatabase() {
        jdbc.update("UPDATE stock_import_rows SET posted_stock_transaction_id=NULL");
        jdbc.update("DELETE FROM loss_records");
        jdbc.update("DELETE FROM damage_records");
        jdbc.update("DELETE FROM site_transfer_items");
        jdbc.update("DELETE FROM site_transfers");
        jdbc.update("DELETE FROM stock_transactions WHERE reversal_of_transaction_id IS NOT NULL");
        jdbc.update("DELETE FROM stock_transactions");
        jdbc.update("DELETE FROM stock_import_location_mappings");
        jdbc.update("DELETE FROM stock_import_rows");
        jdbc.update("DELETE FROM stock_import_batches");
        jdbc.update("DELETE FROM item_aliases");
        jdbc.update("DELETE FROM receiving_challan_items");
        jdbc.update("DELETE FROM receiving_challans");
        jdbc.update("DELETE FROM issued_challan_items");
        jdbc.update("DELETE FROM issued_challans");
        jdbc.update("DELETE FROM site_order_items");
        jdbc.update("DELETE FROM site_orders");
        jdbc.update("DELETE FROM agreement_items");
        jdbc.update("DELETE FROM agreements");
        jdbc.update("DELETE FROM quotation_items");
        jdbc.update("DELETE FROM quotations");
        jdbc.update("DELETE FROM agreement_templates");
        jdbc.update("DELETE FROM purchase_items");
        jdbc.update("DELETE FROM scrap_items");
        jdbc.update("DELETE FROM stock_adjustment_items");
        jdbc.update("DELETE FROM purchases");
        jdbc.update("DELETE FROM scrap_entries");
        jdbc.update("DELETE FROM stock_adjustments");
        jdbc.update("DELETE FROM stock_balances");
        jdbc.update("DELETE FROM site_stock_balances");
        jdbc.update("DELETE FROM file_attachments");
        jdbc.update("DELETE FROM user_activity_logs");
        sites.deleteAll();
        items.deleteAll();
        categories.deleteAll();
        parties.deleteAll();
    }

    private record PreparedBatch(long id, String checksum) {}
}
