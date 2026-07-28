package com.stocksync.phase6b;

import com.fasterxml.jackson.databind.*;
import com.stocksync.BaseIntegrationTest;
import com.stocksync.challan.dto.*;
import com.stocksync.challan.repository.ReceivingChallanRepository;
import com.stocksync.inventory.entity.*;
import com.stocksync.inventory.repository.*;
import com.stocksync.party.entity.*;
import com.stocksync.party.repository.*;
import com.stocksync.site.entity.*;
import com.stocksync.site.repository.SiteRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WithMockUser(username="operations",roles="OPERATIONS")
class Phase6BIntegrationTest extends BaseIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PartyRepository parties;
    @Autowired SiteRepository sites;
    @Autowired ItemCategoryRepository categories;
    @Autowired ItemRepository items;
    @Autowired StockBalanceRepository balances;
    @Autowired SiteStockBalanceRepository siteBalances;
    @Autowired ReceivingChallanRepository challans;

    private Party party;
    private Site site;
    private Item item;

    @BeforeEach void setup(){
        jdbc.update("UPDATE stock_import_rows SET posted_stock_transaction_id=NULL");
        jdbc.update("DELETE FROM loss_records");
        jdbc.update("DELETE FROM damage_records");
        jdbc.update("DELETE FROM site_transfer_items");
        jdbc.update("DELETE FROM site_transfers");
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
        jdbc.update("DELETE FROM quotation_templates");
        jdbc.update("DELETE FROM stock_transactions");
        jdbc.update("DELETE FROM purchase_items");
        jdbc.update("DELETE FROM scrap_items");
        jdbc.update("DELETE FROM stock_adjustment_items");
        jdbc.update("DELETE FROM purchases");
        jdbc.update("DELETE FROM scrap_entries");
        jdbc.update("DELETE FROM stock_adjustments");
        jdbc.update("DELETE FROM stock_balances");
        jdbc.update("DELETE FROM site_stock_balances");
        
        items.deleteAll();
        categories.deleteAll();
        sites.deleteAll();
        parties.deleteAll();

        party = new Party();
        party.setLegalName("Phase Six Return Construction");
        party.setTradeName("Six Return Trade");
        party.setContactPerson("Rohan");
        party.setPhone("9988776655");
        party.setEmail("rohan@six.test");
        party.setAddress("Pune");
        party.setState("Maharashtra");
        party.setCreatedBy("test");
        party.setUpdatedBy("test");
        party = parties.save(party);

        site = new Site();
        site.setParty(party);
        site.setSiteName("Phase Six Return Site");
        site.setSiteCode("P6-RET-SITE");
        site.setAddress("Wakad");
        site.setContactPerson("Rohan");
        site.setStartDate(LocalDate.now());
        site.setStatus(SiteStatus.ACTIVE);
        site.setCreatedBy("test");
        site.setUpdatedBy("test");
        site = sites.save(site);

        ItemCategory cat = new ItemCategory();
        cat.setName("Plates");
        cat.setActive(true);
        cat.setCreatedBy("test");
        cat.setUpdatedBy("test");
        cat = categories.save(cat);

        item = new Item();
        item.setItemCode("P6-RET-PLATE");
        item.setItemName("Plate");
        item.setCategory(cat);
        item.setUnit("PCS");
        item.setMinimumStock(BigDecimal.TEN);
        item.setWeightPerPiece(new BigDecimal("12.5"));
        item.setActive(true);
        item.setCreatedBy("test");
        item.setUpdatedBy("test");
        item = items.save(item);
    }

    @Test
    void receivingChallanLifecycleWithLegacyReturnsAndReversals() throws Exception {
        // Set initial site stock balance (e.g. from Opening Stock Import) and global balance via direct SQL
        jdbc.update("INSERT INTO site_stock_balances(site_id, item_id, pending_quantity, version) VALUES(?, ?, 30.0000, 0)", site.getId(), item.getId());
        jdbc.update("INSERT INTO stock_balances(item_id, available_quantity, issued_quantity, version) VALUES(?, 0, 30.0000, 0)", item.getId());

        // 1. Create a draft receiving challan for legacy return
        ReceivingChallanRequest req = new ReceivingChallanRequest(
                null,
                party.getId(),
                site.getId(),
                null,
                LocalDate.now(),
                "MH-12-RC-1122",
                "Shrikant",
                "9876543210",
                null,
                "OPENING_SITE_BALANCE",
                "Legacy items return",
                List.of(new ReceivingChallanItemRequest(
                        item.getId(),
                        null,
                        null,
                        new BigDecimal("20.0000"), // good
                        new BigDecimal("5.0000"),  // damaged
                        new BigDecimal("3.0000"),  // lost
                        BigDecimal.ZERO,
                        null,
                        null,
                        BigDecimal.ZERO,
                        "Returned mixed quantities"
                ))
        );

        String responseStr = mvc.perform(post("/api/v1/challans/receiving")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.receivingChallanNumber").value("RC/2026-27/0001"))
                .andReturn().getResponse().getContentAsString();

        JsonNode resObj = json.readTree(responseStr);
        Long challanId = resObj.get("id").asLong();

        // 2. Post the receiving challan
        mvc.perform(post("/api/v1/challans/receiving/" + challanId + "/post")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("POSTED"));

        // 3. Verify stock updates
        SiteStockBalance sbUpdated = siteBalances.findBySiteIdAndItemId(site.getId(), item.getId()).orElseThrow();
        Assertions.assertEquals(new BigDecimal("2.0000").stripTrailingZeros(), sbUpdated.getPendingQuantity().stripTrailingZeros()); // 30 - (20 + 5 + 3) = 2

        StockBalance gbUpdated = balances.findById(item.getId()).orElseThrow();
        Assertions.assertEquals(new BigDecimal("20.0000").stripTrailingZeros(), gbUpdated.getAvailableQuantity().stripTrailingZeros()); // Good returned
        Assertions.assertEquals(new BigDecimal("3.0000").stripTrailingZeros(), gbUpdated.getLostQuantity().stripTrailingZeros()); // Lost quantity
        Assertions.assertEquals(new BigDecimal("2.0000").stripTrailingZeros(), gbUpdated.getIssuedQuantity().stripTrailingZeros()); // Issued remaining

        // 4. Cancel & reverse the posted challan as ADMIN
        mvc.perform(post("/api/v1/challans/receiving/" + challanId + "/cancel")
                .with(csrf())
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cancellationReason\":\"User error, double entry\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // 5. Verify balances are reversed to initial values
        SiteStockBalance sbReversed = siteBalances.findBySiteIdAndItemId(site.getId(), item.getId()).orElseThrow();
        Assertions.assertEquals(new BigDecimal("30.0000").stripTrailingZeros(), sbReversed.getPendingQuantity().stripTrailingZeros());

        StockBalance gbReversed = balances.findById(item.getId()).orElseThrow();
        Assertions.assertEquals(BigDecimal.ZERO.stripTrailingZeros(), gbReversed.getAvailableQuantity().stripTrailingZeros());
        Assertions.assertEquals(BigDecimal.ZERO.stripTrailingZeros(), gbReversed.getLostQuantity().stripTrailingZeros());
        Assertions.assertEquals(new BigDecimal("30.0000").stripTrailingZeros(), gbReversed.getIssuedQuantity().stripTrailingZeros());
    }
}
