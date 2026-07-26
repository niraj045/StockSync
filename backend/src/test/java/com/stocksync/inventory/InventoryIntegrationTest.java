package com.stocksync.inventory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksync.BaseIntegrationTest;
import com.stocksync.inventory.entity.*;
import com.stocksync.inventory.repository.*;
import com.stocksync.party.entity.Vendor;
import com.stocksync.party.repository.VendorRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WithMockUser(username="operations",roles="OPERATIONS")
class InventoryIntegrationTest extends BaseIntegrationTest {
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate jdbc;
    @Autowired ItemRepository items;@Autowired ItemCategoryRepository categories;@Autowired VendorRepository vendors;
    private Item item;private Vendor vendor;

    @BeforeEach void setup(){
        jdbc.update("UPDATE stock_import_rows SET posted_stock_transaction_id=NULL");
        jdbc.update("DELETE FROM site_order_items");jdbc.update("DELETE FROM site_orders");jdbc.update("DELETE FROM agreement_items");
        jdbc.update("DELETE FROM agreements");jdbc.update("DELETE FROM quotation_items");jdbc.update("DELETE FROM quotations");
        jdbc.update("DELETE FROM agreement_templates");
        jdbc.update("DELETE FROM stock_transactions");jdbc.update("DELETE FROM purchase_items");jdbc.update("DELETE FROM scrap_items");
        jdbc.update("DELETE FROM stock_adjustment_items");jdbc.update("DELETE FROM purchases");jdbc.update("DELETE FROM scrap_entries");
        jdbc.update("DELETE FROM stock_adjustments");jdbc.update("DELETE FROM stock_balances");
        jdbc.update("DELETE FROM stock_import_location_mappings");jdbc.update("DELETE FROM stock_import_rows");
        jdbc.update("DELETE FROM stock_import_batches");jdbc.update("DELETE FROM item_aliases");
        items.deleteAll();categories.deleteAll();vendors.deleteAll();
        ItemCategory c=new ItemCategory();c.setName("Inventory Test");c.setActive(true);c.setCreatedBy("test");c.setUpdatedBy("test");c=categories.save(c);
        item=new Item();item.setItemCode("INV-01");item.setItemName("Inventory Item");item.setCategory(c);item.setUnit("PCS");
        item.setMinimumStock(BigDecimal.TEN);item.setWeightPerPiece(new BigDecimal("2.5"));item.setActive(true);item.setCreatedBy("test");item.setUpdatedBy("test");item=items.save(item);
        vendor=new Vendor();vendor.setName("Inventory Vendor");vendor.setActive(true);vendor.setCreatedBy("test");vendor.setUpdatedBy("test");vendor=vendors.save(vendor);
    }

    @Test void purchaseCreatesImmutableLedgerAndIncreasesBalance() throws Exception {
        String key=UUID.randomUUID().toString();
        String body="""
            {"vendorId":%d,"purchaseDate":"2026-07-26","notes":"Opening stock",
             "items":[{"itemId":%d,"quantity":100,"unitRate":50}]}
            """.formatted(vendor.getId(),item.getId());
        String first=mvc.perform(post("/api/v1/stock/purchases").with(csrf()).header("Idempotency-Key",key)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(false)).andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/v1/stock/purchases").with(csrf()).header("Idempotency-Key",key)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk())
                .andExpect(jsonPath("$.replayed").value(true));
        mvc.perform(get("/api/v1/stock/balances")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].availableQuantity").value(100.0))
                .andExpect(jsonPath("$.content[0].availableWeight").value(250.0));
        mvc.perform(get("/api/v1/stock/transactions")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sourceId").value(json.readTree(first).get("id").asLong()));
    }

    @Test void scrapAndAdjustmentMaintainProjection() throws Exception {
        purchase(new BigDecimal("50"));
        mvc.perform(post("/api/v1/stock/scrap").with(csrf()).header("Idempotency-Key",UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"date":"2026-07-26","reason":"Bent beyond repair","items":[{"itemId":%d,"quantity":5}]}
                    """.formatted(item.getId()))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/stock/adjustments").with(csrf()).header("Idempotency-Key",UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"date":"2026-07-26","direction":"IN","reason":"Physical count correction",
                     "items":[{"itemId":%d,"quantity":2}]}
                    """.formatted(item.getId()))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/stock/balances")).andExpect(jsonPath("$.content[0].availableQuantity").value(47.0))
                .andExpect(jsonPath("$.content[0].scrappedQuantity").value(5.0));
    }

    @Test void negativeStockIsBlockedAndTransactionRollsBack() throws Exception {
        purchase(BigDecimal.TEN);
        mvc.perform(post("/api/v1/stock/adjustments").with(csrf()).header("Idempotency-Key",UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"date":"2026-07-26","direction":"OUT","reason":"Count correction",
                     "items":[{"itemId":%d,"quantity":11}]}
                    """.formatted(item.getId()))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
        mvc.perform(get("/api/v1/stock/balances")).andExpect(jsonPath("$.content[0].availableQuantity").value(10.0));
        Assertions.assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM stock_adjustments",Integer.class));
    }

    @Test void viewerCannotPostStock() throws Exception {
        mvc.perform(post("/api/v1/stock/adjustments").with(csrf())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("viewer").roles("VIEWER"))
                .header("Idempotency-Key",UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"date\":\"2026-07-26\",\"direction\":\"IN\",\"reason\":\"No access\",\"items\":[{\"itemId\":1,\"quantity\":1}]}"))
                .andExpect(status().isForbidden());
    }

    @Test void concurrentOutboundAdjustmentsCannotOversell() throws Exception {
        purchase(BigDecimal.TEN);
        String body="""
            {"date":"2026-07-26","direction":"OUT","reason":"Concurrent count",
             "items":[{"itemId":%d,"quantity":8}]}
            """.formatted(item.getId());
        try (ExecutorService executor=Executors.newFixedThreadPool(2)) {
            Callable<Integer> request=()->mvc.perform(post("/api/v1/stock/adjustments").with(csrf())
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("operations").roles("OPERATIONS"))
                    .header("Idempotency-Key",UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andReturn().getResponse().getStatus();
            Future<Integer> first=executor.submit(request);Future<Integer> second=executor.submit(request);
            var statuses=java.util.List.of(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS));
            Assertions.assertEquals(1,statuses.stream().filter(s->s==200).count(),"statuses="+statuses);
            Assertions.assertEquals(1,statuses.stream().filter(s->s==400).count(),"statuses="+statuses);
        }
        mvc.perform(get("/api/v1/stock/balances")).andExpect(jsonPath("$.content[0].availableQuantity").value(2.0));
    }
    private void purchase(BigDecimal quantity)throws Exception{
        mvc.perform(post("/api/v1/stock/purchases").with(csrf()).header("Idempotency-Key",UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"vendorId":%d,"purchaseDate":"2026-07-26","items":[{"itemId":%d,"quantity":%s,"unitRate":50}]}
                    """.formatted(vendor.getId(),item.getId(),quantity))).andExpect(status().isOk());
    }
}
