package com.stocksync.phase6c;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksync.BaseIntegrationTest;
import com.stocksync.agreement.entity.Agreement;
import com.stocksync.agreement.entity.AgreementItem;
import com.stocksync.agreement.entity.AgreementStatus;
import com.stocksync.agreement.repository.AgreementRepository;
import com.stocksync.challan.dto.*;
import com.stocksync.challan.repository.ReceivingChallanRepository;
import com.stocksync.exception.dto.*;
import com.stocksync.exception.entity.LossStatus;
import com.stocksync.exception.repository.ItemExchangeRepository;
import com.stocksync.exception.repository.SiteTransferRepository;
import com.stocksync.exception.repository.StockDamageRepository;
import com.stocksync.exception.repository.StockLossRepository;
import com.stocksync.inventory.entity.*;
import com.stocksync.inventory.repository.*;
import com.stocksync.party.entity.Party;
import com.stocksync.party.repository.PartyRepository;
import com.stocksync.site.entity.Site;
import com.stocksync.site.entity.SiteStatus;
import com.stocksync.site.repository.SiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = {"ADMIN"})
class Phase6CIntegrationTest extends BaseIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PartyRepository parties;
    @Autowired SiteRepository sites;
    @Autowired ItemCategoryRepository categories;
    @Autowired ItemRepository items;
    @Autowired StockBalanceRepository balances;
    @Autowired SiteStockBalanceRepository siteBalances;
    @Autowired AgreementRepository agreements;
    @Autowired StockLossRepository losses;
    @Autowired StockDamageRepository damages;
    @Autowired ItemExchangeRepository exchanges;
    @Autowired SiteTransferRepository transfers;
    @Autowired ReceivingChallanRepository receivingChallans;

    private Party party;
    private Site site;
    private Site site2;
    private Item expectedItem;
    private Item actualItem;
    private Agreement agreement;
    private Agreement agreement2;

    @BeforeEach
    void setup() {
        // Clean up tables
        jdbc.update("UPDATE stock_import_rows SET posted_stock_transaction_id = NULL");
        jdbc.update("DELETE FROM loss_records");
        jdbc.update("DELETE FROM damage_records");
        jdbc.update("DELETE FROM item_exchange_records");
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
        jdbc.update("DELETE FROM stock_balances");
        jdbc.update("DELETE FROM site_stock_balances");

        items.deleteAll();
        categories.deleteAll();
        sites.deleteAll();
        parties.deleteAll();

        // 1. Create party
        party = new Party();
        party.setLegalName("Steel Structures Ltd");
        party.setTradeName("Steel Structures");
        party.setContactPerson("Aniket");
        party.setPhone("9988001122");
        party.setEmail("aniket@steel.test");
        party.setAddress("Mumbai");
        party.setState("Maharashtra");
        party.setCreatedBy("test");
        party.setUpdatedBy("test");
        party = parties.save(party);

        // 2. Create sites
        site = new Site();
        site.setParty(party);
        site.setSiteName("Worli Site");
        site.setSiteCode("WORLI-01");
        site.setAddress("Worli, Mumbai");
        site.setContactPerson("Aniket");
        site.setStartDate(LocalDate.now());
        site.setStatus(SiteStatus.ACTIVE);
        site.setCreatedBy("test");
        site.setUpdatedBy("test");
        site = sites.save(site);

        site2 = new Site();
        site2.setParty(party);
        site2.setSiteName("Bandra Site");
        site2.setSiteCode("BANDRA-02");
        site2.setAddress("Bandra, Mumbai");
        site2.setContactPerson("Aniket");
        site2.setStartDate(LocalDate.now());
        site2.setStatus(SiteStatus.ACTIVE);
        site2.setCreatedBy("test");
        site2.setUpdatedBy("test");
        site2 = sites.save(site2);

        // 3. Create category & items
        ItemCategory cat = new ItemCategory();
        cat.setName("Frames");
        cat.setActive(true);
        cat.setCreatedBy("test");
        cat.setUpdatedBy("test");
        cat = categories.save(cat);

        expectedItem = new Item();
        expectedItem.setItemCode("FRAME-10");
        expectedItem.setItemName("Frame 10ft");
        expectedItem.setCategory(cat);
        expectedItem.setUnit("PCS");
        expectedItem.setWeightPerPiece(new BigDecimal("10.0000"));
        expectedItem.setActive(true);
        expectedItem.setCreatedBy("test");
        expectedItem.setUpdatedBy("test");
        expectedItem = items.save(expectedItem);

        actualItem = new Item();
        actualItem.setItemCode("FRAME-12");
        actualItem.setItemName("Frame 12ft");
        actualItem.setCategory(cat);
        actualItem.setUnit("PCS");
        actualItem.setWeightPerPiece(new BigDecimal("12.0000"));
        actualItem.setActive(true);
        actualItem.setCreatedBy("test");
        actualItem.setUpdatedBy("test");
        actualItem = items.save(actualItem);

        // 4. Create agreements
        agreement = new Agreement();
        agreement.setAgreementNumber("AGR/2026-27/0001");
        agreement.setParty(party);
        agreement.setSite(site);
        agreement.setPartyLegalNameSnapshot(party.getLegalName());
        agreement.setSiteNameSnapshot(site.getSiteName());
        agreement.setSiteCodeSnapshot(site.getSiteCode());
        agreement.setAgreementDate(LocalDate.now());
        agreement.setEffectiveDate(LocalDate.now());
        agreement.setStatus(com.stocksync.agreement.entity.AgreementStatus.ACTIVE);
        agreement.setRentalType(com.stocksync.quotation.entity.RentalType.PER_PIECE_PER_DAY);
        agreement.setCreatedBy("test");
        agreement.setUpdatedBy("test");

        AgreementItem ai1 = new AgreementItem();
        ai1.setAgreement(agreement);
        ai1.setItem(expectedItem);
        ai1.setItemCodeSnapshot(expectedItem.getItemCode());
        ai1.setItemNameSnapshot(expectedItem.getItemName());
        ai1.setUnitSnapshot(expectedItem.getUnit());
        ai1.setAgreedQuantity(new BigDecimal("100.0000"));
        ai1.setRentalRate(new BigDecimal("2.5000"));
        ai1.setRentalType(com.stocksync.quotation.entity.RentalType.PER_PIECE_PER_DAY);
        ai1.setUnitRate(new BigDecimal("100.0000"));
        ai1.setLossRatePerPiece(new BigDecimal("150.0000"));
        ai1.setLossRatePerWeight(new BigDecimal("15.0000"));
        ai1.setDamageRate(new BigDecimal("50.0000"));
        ai1.setSequence(1);
        agreement.addItem(ai1);

        AgreementItem ai2 = new AgreementItem();
        ai2.setAgreement(agreement);
        ai2.setItem(actualItem);
        ai2.setItemCodeSnapshot(actualItem.getItemCode());
        ai2.setItemNameSnapshot(actualItem.getItemName());
        ai2.setUnitSnapshot(actualItem.getUnit());
        ai2.setAgreedQuantity(new BigDecimal("100.0000"));
        ai2.setRentalRate(new BigDecimal("3.0000"));
        ai2.setRentalType(com.stocksync.quotation.entity.RentalType.PER_PIECE_PER_DAY);
        ai2.setUnitRate(new BigDecimal("120.0000"));
        ai2.setLossRatePerPiece(new BigDecimal("180.0000"));
        ai2.setLossRatePerWeight(new BigDecimal("18.0000"));
        ai2.setDamageRate(new BigDecimal("60.0000"));
        ai2.setSequence(2);
        agreement.addItem(ai2);

        agreement = agreements.save(agreement);

        agreement2 = new Agreement();
        agreement2.setAgreementNumber("AGR/2026-27/0002");
        agreement2.setParty(party);
        agreement2.setSite(site2);
        agreement2.setPartyLegalNameSnapshot(party.getLegalName());
        agreement2.setSiteNameSnapshot(site2.getSiteName());
        agreement2.setSiteCodeSnapshot(site2.getSiteCode());
        agreement2.setAgreementDate(LocalDate.now());
        agreement2.setEffectiveDate(LocalDate.now());
        agreement2.setStatus(com.stocksync.agreement.entity.AgreementStatus.ACTIVE);
        agreement2.setRentalType(com.stocksync.quotation.entity.RentalType.PER_PIECE_PER_DAY);
        agreement2.setCreatedBy("test");
        agreement2.setUpdatedBy("test");

        AgreementItem ai3 = new AgreementItem();
        ai3.setAgreement(agreement2);
        ai3.setItem(expectedItem);
        ai3.setItemCodeSnapshot(expectedItem.getItemCode());
        ai3.setItemNameSnapshot(expectedItem.getItemName());
        ai3.setUnitSnapshot(expectedItem.getUnit());
        ai3.setAgreedQuantity(new BigDecimal("100.0000"));
        ai3.setRentalRate(new BigDecimal("2.5000"));
        ai3.setRentalType(com.stocksync.quotation.entity.RentalType.PER_PIECE_PER_DAY);
        ai3.setUnitRate(new BigDecimal("100.0000"));
        ai3.setLossRatePerPiece(new BigDecimal("150.0000"));
        ai3.setLossRatePerWeight(new BigDecimal("15.0000"));
        ai3.setDamageRate(new BigDecimal("50.0000"));
        ai3.setSequence(1);
        agreement2.addItem(ai3);

        agreement2 = agreements.save(agreement2);
    }

    @Test
    void testStockLossDraftApproveAndReverse() throws Exception {
        // Setup initial stock: 50 pieces issued, 50 pending on site
        jdbc.update("INSERT INTO site_stock_balances(site_id, item_id, pending_quantity, version) VALUES(?, ?, 50.0000, 0)", site.getId(), expectedItem.getId());
        jdbc.update("INSERT INTO stock_balances(item_id, available_quantity, issued_quantity, version) VALUES(?, 0, 50.0000, 0)", expectedItem.getId());

        StockLossRequest req = new StockLossRequest(
                null,
                agreement.getId(),
                party.getId(),
                site.getId(),
                expectedItem.getId(),
                LocalDate.now(),
                new BigDecimal("10.0000"),
                new BigDecimal("100.0000"),
                "PER_PIECE",
                new BigDecimal("150.0000"),
                "Lost in transition",
                null
        );

        // 1. Create Stock Loss Draft
        String createJson = mvc.perform(post("/api/v1/stock-losses")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        StockLossResponse created = json.readValue(createJson, StockLossResponse.class);
        assertNotNull(created.id());
        assertEquals("DRAFT", created.status());
        assertEquals(new BigDecimal("1500.0000").doubleValue(), created.calculatedRecoveryAmount().doubleValue());

        // 2. Approve Stock Loss
        String approveJson = mvc.perform(post("/api/v1/stock-losses/" + created.id() + "/approve")
                .with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        StockLossResponse approved = json.readValue(approveJson, StockLossResponse.class);
        assertEquals("APPROVED", approved.status());

        // Verify stock deductions
        SiteStockBalance sBalance = siteBalances.findBySiteIdAndItemId(site.getId(), expectedItem.getId()).orElseThrow();
        assertEquals(new BigDecimal("40.0000").doubleValue(), sBalance.getPendingQuantity().doubleValue());

        StockBalance gBalance = balances.findById(expectedItem.getId()).orElseThrow();
        assertEquals(new BigDecimal("10.0000").doubleValue(), gBalance.getLostQuantity().doubleValue());

        // 3. Reverse Stock Loss
        String reverseJson = mvc.perform(post("/api/v1/stock-losses/" + created.id() + "/reverse")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(java.util.Map.of("reversalReason", "Found items later"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        StockLossResponse reversed = json.readValue(reverseJson, StockLossResponse.class);
        assertEquals("REVERSED", reversed.status());
        assertEquals("Found items later", reversed.reversalReason());

        // Verify stock restored
        sBalance = siteBalances.findBySiteIdAndItemId(site.getId(), expectedItem.getId()).orElseThrow();
        assertEquals(new BigDecimal("50.0000").doubleValue(), sBalance.getPendingQuantity().doubleValue());

        gBalance = balances.findById(expectedItem.getId()).orElseThrow();
        assertEquals(new BigDecimal("0.0000").doubleValue(), gBalance.getLostQuantity().doubleValue());
    }

    @Test
    void testStockDamageLifecycle() throws Exception {
        jdbc.update("INSERT INTO site_stock_balances(site_id, item_id, pending_quantity, version) VALUES(?, ?, 30.0000, 0)", site.getId(), expectedItem.getId());
        jdbc.update("INSERT INTO stock_balances(item_id, available_quantity, issued_quantity, version) VALUES(?, 0, 30.0000, 0)", expectedItem.getId());

        StockDamageRequest req = new StockDamageRequest(
                null,
                agreement.getId(),
                party.getId(),
                site.getId(),
                expectedItem.getId(),
                LocalDate.now(),
                new BigDecimal("5.0000"),
                new BigDecimal("50.0000"),
                true,
                "BENT",
                "Bent during concrete flow",
                "PER_PIECE",
                new BigDecimal("50.0000"),
                new BigDecimal("100.0000"),
                BigDecimal.ZERO,
                null
        );

        // 1. Create damage draft
        String cJson = mvc.perform(post("/api/v1/stock-damages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        StockDamageResponse created = json.readValue(cJson, StockDamageResponse.class);
        assertEquals("DRAFT", created.status());

        // 2. Post/Record damage
        String recJson = mvc.perform(post("/api/v1/stock-damages/" + created.id() + "/record")
                .with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        StockDamageResponse recorded = json.readValue(recJson, StockDamageResponse.class);
        assertEquals("RECORDED", recorded.status());

        // Check stock
        SiteStockBalance sB = siteBalances.findBySiteIdAndItemId(site.getId(), expectedItem.getId()).orElseThrow();
        assertEquals(new BigDecimal("25.0000").doubleValue(), sB.getPendingQuantity().doubleValue());

        StockBalance gB = balances.findById(expectedItem.getId()).orElseThrow();
        assertEquals(new BigDecimal("5.0000").doubleValue(), gB.getDamagedQuantity().doubleValue());

        // 3. Start repair
        String repJson = mvc.perform(post("/api/v1/stock-damages/" + created.id() + "/start-repair")
                .with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        StockDamageResponse underRepair = json.readValue(repJson, StockDamageResponse.class);
        assertEquals("UNDER_REPAIR", underRepair.status());

        // 4. Mark Repaired
        String compJson = mvc.perform(post("/api/v1/stock-damages/" + created.id() + "/mark-repaired")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(java.util.Map.of("actualRepairCost", new BigDecimal("120.0000")))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        StockDamageResponse repaired = json.readValue(compJson, StockDamageResponse.class);
        assertEquals("REPAIRED", repaired.status());
        assertEquals(new BigDecimal("120.0000").doubleValue(), repaired.actualRepairCost().doubleValue());

        // Damaged stock should decrease, available stock should increase globally
        gB = balances.findById(expectedItem.getId()).orElseThrow();
        assertEquals(new BigDecimal("0.0000").doubleValue(), gB.getDamagedQuantity().doubleValue());
        assertEquals(new BigDecimal("5.0000").doubleValue(), gB.getAvailableQuantity().doubleValue());
    }

    @Test
    void testItemExchangePostAndCancel() throws Exception {
        // expectedItem has 10 pending on site, actualItem has 20 available in godown
        jdbc.update("INSERT INTO site_stock_balances(site_id, item_id, pending_quantity, version) VALUES(?, ?, 10.0000, 0)", site.getId(), expectedItem.getId());
        jdbc.update("INSERT INTO stock_balances(item_id, available_quantity, issued_quantity, version) VALUES(?, 0, 10.0000, 0)", expectedItem.getId());
        jdbc.update("INSERT INTO stock_balances(item_id, available_quantity, issued_quantity, version) VALUES(?, 20.0000, 0, 0)", actualItem.getId());

        ItemExchangeRequest req = new ItemExchangeRequest(
                null,
                agreement.getId(),
                party.getId(),
                site.getId(),
                expectedItem.getId(),
                actualItem.getId(),
                LocalDate.now(),
                new BigDecimal("5.0000"), // expected quantity
                new BigDecimal("5.0000"), // actual quantity
                new BigDecimal("50.0000"),
                new BigDecimal("60.0000"),
                "AVAILABLE",
                "Wrong size returned"
        );

        // 1. Create exchange draft
        String cJson = mvc.perform(post("/api/v1/item-exchanges")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ItemExchangeResponse created = json.readValue(cJson, ItemExchangeResponse.class);
        assertEquals("DRAFT", created.status());

        // 2. Post exchange
        String postJson = mvc.perform(post("/api/v1/item-exchanges/" + created.id() + "/post")
                .with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ItemExchangeResponse posted = json.readValue(postJson, ItemExchangeResponse.class);
        assertEquals("POSTED", posted.status());

        // Expected item site pending decreases by 5, issued decreases by 5
        SiteStockBalance expSiteB = siteBalances.findBySiteIdAndItemId(site.getId(), expectedItem.getId()).orElseThrow();
        assertEquals(new BigDecimal("5.0000").doubleValue(), expSiteB.getPendingQuantity().doubleValue());
        
        StockBalance expB = balances.findById(expectedItem.getId()).orElseThrow();
        assertEquals(new BigDecimal("5.0000").doubleValue(), expB.getIssuedQuantity().doubleValue());

        // Actual item available stock increases by 5 (20 -> 25)
        StockBalance actB = balances.findById(actualItem.getId()).orElseThrow();
        assertEquals(new BigDecimal("25.0000").doubleValue(), actB.getAvailableQuantity().doubleValue());

        // 3. Cancel/Reverse exchange
        String cancelJson = mvc.perform(post("/api/v1/item-exchanges/" + created.id() + "/cancel")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(java.util.Map.of("cancellationReason", "Post error"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ItemExchangeResponse cancelled = json.readValue(cancelJson, ItemExchangeResponse.class);
        assertEquals("CANCELLED", cancelled.status());

        // Assert all values restored
        expSiteB = siteBalances.findBySiteIdAndItemId(site.getId(), expectedItem.getId()).orElseThrow();
        assertEquals(new BigDecimal("10.0000").doubleValue(), expSiteB.getPendingQuantity().doubleValue());

        expB = balances.findById(expectedItem.getId()).orElseThrow();
        assertEquals(new BigDecimal("10.0000").doubleValue(), expB.getIssuedQuantity().doubleValue());

        actB = balances.findById(actualItem.getId()).orElseThrow();
        assertEquals(new BigDecimal("20.0000").doubleValue(), actB.getAvailableQuantity().doubleValue());
    }

    @Test
    void testSiteToSiteTransfer() throws Exception {
        // site 1 (source) has 30 pending of expectedItem
        jdbc.update("INSERT INTO site_stock_balances(site_id, item_id, pending_quantity, version) VALUES(?, ?, 30.0000, 0)", site.getId(), expectedItem.getId());
        jdbc.update("INSERT INTO site_stock_balances(site_id, item_id, pending_quantity, version) VALUES(?, ?, 0.0000, 0)", site2.getId(), expectedItem.getId());
        jdbc.update("INSERT INTO stock_balances(item_id, available_quantity, issued_quantity, version) VALUES(?, 0, 30.0000, 0)", expectedItem.getId());

        AgreementItem srcAgItem = agreement.getItems().stream().filter(i -> i.getItem().getId().equals(expectedItem.getId())).findFirst().orElseThrow();
        AgreementItem destAgItem = agreement2.getItems().stream().filter(i -> i.getItem().getId().equals(expectedItem.getId())).findFirst().orElseThrow();

        SiteTransferRequest req = new SiteTransferRequest(
                null,
                agreement.getId(),
                agreement2.getId(),
                party.getId(),
                site.getId(),
                party.getId(),
                site2.getId(),
                LocalDate.now(),
                "MH-12-ST-8888",
                "Kunal",
                "9822334455",
                null,
                "Transferring frame panels",
                List.of(new SiteTransferItemRequest(
                        srcAgItem.getId(),
                        destAgItem.getId(),
                        expectedItem.getId(),
                        new BigDecimal("15.0000"),
                        new BigDecimal("150.0000")
                ))
        );

        // 1. Create transfer draft
        String cJson = mvc.perform(post("/api/v1/site-transfers")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        SiteTransferResponse created = json.readValue(cJson, SiteTransferResponse.class);
        assertEquals("DRAFT", created.status());
        assertEquals(1, created.items().size());

        // 2. Post transfer
        String postJson = mvc.perform(post("/api/v1/site-transfers/" + created.id() + "/post")
                .with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        SiteTransferResponse posted = json.readValue(postJson, SiteTransferResponse.class);
        assertEquals("POSTED", posted.status());

        // Verify balances: source site reduces by 15, destination site increases by 15
        SiteStockBalance srcB = siteBalances.findBySiteIdAndItemId(site.getId(), expectedItem.getId()).orElseThrow();
        assertEquals(new BigDecimal("15.0000").doubleValue(), srcB.getPendingQuantity().doubleValue());

        SiteStockBalance destB = siteBalances.findBySiteIdAndItemId(site2.getId(), expectedItem.getId()).orElseThrow();
        assertEquals(new BigDecimal("15.0000").doubleValue(), destB.getPendingQuantity().doubleValue());

        // Global balances remain same
        StockBalance gB = balances.findById(expectedItem.getId()).orElseThrow();
        assertEquals(new BigDecimal("30.0000").doubleValue(), gB.getIssuedQuantity().doubleValue());

        // 3. Cancel transfer
        String cancelJson = mvc.perform(post("/api/v1/site-transfers/" + created.id() + "/cancel")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(java.util.Map.of("cancellationReason", "Truck broke down"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        SiteTransferResponse cancelled = json.readValue(cancelJson, SiteTransferResponse.class);
        assertEquals("CANCELLED", cancelled.status());

        // Balances restored
        srcB = siteBalances.findBySiteIdAndItemId(site.getId(), expectedItem.getId()).orElseThrow();
        assertEquals(new BigDecimal("30.0000").doubleValue(), srcB.getPendingQuantity().doubleValue());

        destB = siteBalances.findBySiteIdAndItemId(site2.getId(), expectedItem.getId()).orElseThrow();
        assertEquals(new BigDecimal("0.0000").doubleValue(), destB.getPendingQuantity().doubleValue());
    }
}
