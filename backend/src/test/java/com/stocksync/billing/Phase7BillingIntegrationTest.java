package com.stocksync.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksync.BaseIntegrationTest;
import com.stocksync.agreement.entity.*;
import com.stocksync.agreement.repository.AgreementItemSlabRepository;
import com.stocksync.agreement.repository.AgreementRepository;
import com.stocksync.billing.dto.BillingRunDtos.*;
import com.stocksync.billing.dto.InvoiceDtos.*;
import com.stocksync.billing.entity.*;
import com.stocksync.billing.repository.*;
import com.stocksync.challan.entity.*;
import com.stocksync.challan.repository.*;
import com.stocksync.inventory.entity.*;
import com.stocksync.inventory.repository.*;
import com.stocksync.order.entity.*;
import com.stocksync.order.repository.*;
import com.stocksync.party.entity.Party;
import com.stocksync.party.repository.PartyRepository;
import com.stocksync.quotation.entity.*;
import com.stocksync.site.entity.Site;
import com.stocksync.site.entity.SiteStatus;
import com.stocksync.site.repository.SiteRepository;
import org.junit.jupiter.api.AfterEach;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = {"ADMIN", "ACCOUNTS"})
class Phase7BillingIntegrationTest extends BaseIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PartyRepository parties;
    @Autowired SiteRepository sites;
    @Autowired ItemCategoryRepository categories;
    @Autowired ItemRepository items;
    @Autowired AgreementRepository agreements;
    @Autowired AgreementItemSlabRepository slabs;
    @Autowired SiteOrderRepository siteOrders;
    @Autowired IssuedChallanRepository issuedChallans;
    @Autowired ReceivingChallanRepository receivingChallans;
    @Autowired BillingRunRepository billingRuns;
    @Autowired InvoiceRepository invoices;

    private Party party;
    private Site site;
    private Item item1;
    private Item item2;
    private Agreement agreement;

    @BeforeEach
    void setup() {
        cleanDatabase();

        // Setup base entities
        party = new Party();
        party.setLegalName("India Infrastructure Ltd");
        party.setTradeName("India Infra");
        party.setGstin("24ABCDE1234F1Z0");
        party.setPan("ABCDE1234F");
        party.setAddress("456 Corporate Towers, GIDC");
        party.setState("GUJARAT");
        party.setContactPerson("Raman Patel");
        party.setPhone("9876543210");
        party.setCreatedBy("admin");
        party = parties.save(party);

        site = new Site();
        site.setParty(party);
        site.setSiteName("Metro Station Site");
        site.setSiteCode("SITE-METRO");
        site.setAddress("456 Corporate Towers, GIDC"); // In same state -> CGST+SGST
        site.setContactPerson("Site Engineer");
        site.setStatus(SiteStatus.ACTIVE);
        site.setCreatedBy("admin");
        site = sites.save(site);

        ItemCategory cat = new ItemCategory();
        cat.setName("Scaffolding Tubes");
        cat.setCreatedBy("admin");
        cat = categories.save(cat);

        item1 = new Item();
        item1.setItemCode("MS-TUBE-2M");
        item1.setItemName("MS Tube 2 Meter");
        item1.setCategory(cat);
        item1.setUnit("PCS");
        item1.setWeightPerPiece(new BigDecimal("10.00"));
        item1.setCreatedBy("admin");
        item1 = items.save(item1);

        item2 = new Item();
        item2.setItemCode("MS-TUBE-3M");
        item2.setItemName("MS Tube 3 Meter");
        item2.setCategory(cat);
        item2.setUnit("PCS");
        item2.setWeightPerPiece(new BigDecimal("15.00"));
        item2.setCreatedBy("admin");
        item2 = items.save(item2);

        // Agreement
        agreement = new Agreement();
        agreement.setAgreementNumber("AGR-2026-001");
        agreement.setParty(party);
        agreement.setSite(site);
        agreement.setAgreementDate(LocalDate.of(2026, 6, 1));
        agreement.setEffectiveDate(LocalDate.of(2026, 6, 10));
        agreement.setExpiryDate(LocalDate.of(2026, 7, 31));
        agreement.setRentalType(RentalType.PER_PIECE_PER_DAY);
        agreement.setBillingCycle(BillingCycle.MONTHLY);
        agreement.setStatus(AgreementStatus.ACTIVE);
        agreement.setBillingStartRule(BillingStartRule.ISSUE_DATE_INCLUDED);
        agreement.setBillingEndRule(BillingEndRule.RETURN_DATE_EXCLUDED);
        agreement.setPartyLegalNameSnapshot(party.getLegalName());
        agreement.setPartyAddressSnapshot(party.getAddress());
        agreement.setPartyStateSnapshot(party.getState());
        agreement.setSiteNameSnapshot(site.getSiteName());
        agreement.setSiteCodeSnapshot(site.getSiteCode());
        agreement.setSiteAddressSnapshot(site.getAddress());
        agreement.setCreatedBy("admin");
        agreement.setUpdatedBy("admin");

        AgreementItem ai1 = new AgreementItem();
        ai1.setAgreement(agreement);
        ai1.setItem(item1);
        ai1.setItemCodeSnapshot(item1.getItemCode());
        ai1.setItemNameSnapshot(item1.getItemName());
        ai1.setUnitSnapshot(item1.getUnit());
        ai1.setWeightSnapshot(item1.getWeightPerPiece());
        ai1.setAgreedQuantity(new BigDecimal("100.00"));
        ai1.setUnitRate(new BigDecimal("5.00")); // standard rate
        ai1.setRentalRate(new BigDecimal("5.00"));
        ai1.setRentalType(RentalType.PER_PIECE_PER_DAY);
        ai1.setSequence(1);
        agreement.getItems().add(ai1);

        AgreementItem ai2 = new AgreementItem();
        ai2.setAgreement(agreement);
        ai2.setItem(item2);
        ai2.setItemCodeSnapshot(item2.getItemCode());
        ai2.setItemNameSnapshot(item2.getItemName());
        ai2.setUnitSnapshot(item2.getUnit());
        ai2.setWeightSnapshot(item2.getWeightPerPiece());
        ai2.setAgreedQuantity(new BigDecimal("100.00"));
        ai2.setUnitRate(new BigDecimal("4.00")); // slab base rate fallback
        ai2.setRentalRate(new BigDecimal("4.00"));
        ai2.setRentalType(RentalType.SLAB_BASED);
        ai2.setSequence(2);

        AgreementItemSlab s1 = new AgreementItemSlab();
        s1.setAgreementItem(ai2);
        s1.setStartDay(1);
        s1.setEndDay(10);
        s1.setRate(new BigDecimal("4.00"));
        ai2.getSlabs().add(s1);

        AgreementItemSlab s2 = new AgreementItemSlab();
        s2.setAgreementItem(ai2);
        s2.setStartDay(11);
        s2.setEndDay(null);
        s2.setRate(new BigDecimal("3.00"));
        ai2.getSlabs().add(s2);

        agreement.getItems().add(ai2);
        agreement = agreements.save(agreement);
    }

    @AfterEach
    void cleanup() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        jdbc.update("SET FOREIGN_KEY_CHECKS = 0");
        jdbc.update("DELETE FROM deposit_invoice_allocations");
        jdbc.update("DELETE FROM security_deposit_transactions");
        jdbc.update("DELETE FROM tds_details");
        jdbc.update("DELETE FROM payment_allocations");
        jdbc.update("DELETE FROM payment_receipts");
        jdbc.update("DELETE FROM billing_source_allocations");
        jdbc.update("DELETE FROM invoice_items");
        jdbc.update("DELETE FROM invoices");
        jdbc.update("DELETE FROM billing_run_segments");
        jdbc.update("DELETE FROM billing_run_charges");
        jdbc.update("DELETE FROM billing_runs");
        jdbc.update("DELETE FROM agreement_item_slabs");
        jdbc.update("DELETE FROM receiving_challan_items");
        jdbc.update("DELETE FROM receiving_challans");
        jdbc.update("DELETE FROM issued_challan_items");
        jdbc.update("DELETE FROM issued_challans");
        jdbc.update("DELETE FROM site_order_items");
        jdbc.update("DELETE FROM site_orders");
        jdbc.update("DELETE FROM agreement_items");
        jdbc.update("DELETE FROM agreements");
        jdbc.update("DELETE FROM stock_transactions");
        jdbc.update("DELETE FROM stock_balances");
        jdbc.update("DELETE FROM site_stock_balances");

        items.deleteAll();
        categories.deleteAll();
        sites.deleteAll();
        parties.deleteAll();
        jdbc.update("SET FOREIGN_KEY_CHECKS = 1");
    }

    @Test
    void testEndToEndBillingAndInvoiceLifecycle() throws Exception {
        // 1. Issue Challan
        SiteOrder order = new SiteOrder();
        order.setAgreement(agreement);
        order.setOrderNumber("ORD-001");
        order.setOrderDate(LocalDate.of(2026, 6, 10));
        order.setSite(site);
        order.setParty(party);
        order.setCreatedBy("admin");
        order = siteOrders.save(order);

        IssuedChallan ic = new IssuedChallan();
        ic.setSiteOrder(order);
        ic.setChallanNumber("IC-001");
        ic.setDispatchDate(LocalDate.of(2026, 6, 10));
        ic.setCreatedBy("admin");
        ic.setTransportCharge(new BigDecimal("150.00")); // transport charge to test recovery
        
        IssuedChallanItem ici1 = new IssuedChallanItem();
        ici1.setIssuedChallan(ic);
        ici1.setItem(item1);
        ici1.setQuantity(new BigDecimal("10.00"));
        ici1.setItemCodeSnapshot(item1.getItemCode());
        ici1.setItemNameSnapshot(item1.getItemName());
        ici1.setUnitSnapshot(item1.getUnit());
        ic.getItems().add(ici1);

        IssuedChallanItem ici2 = new IssuedChallanItem();
        ici2.setIssuedChallan(ic);
        ici2.setItem(item2);
        ici2.setQuantity(new BigDecimal("5.00"));
        ici2.setItemCodeSnapshot(item2.getItemCode());
        ici2.setItemNameSnapshot(item2.getItemName());
        ici2.setUnitSnapshot(item2.getUnit());
        ic.getItems().add(ici2);
        issuedChallans.save(ic);

        // 2. Receiving Challan (Return 4 units of Item 1 on 20-Jun)
        ReceivingChallan rc = new ReceivingChallan();
        rc.setAgreement(agreement);
        rc.setReceivingChallanNumber("RC-001");
        rc.setReceiveDate(LocalDate.of(2026, 6, 20));
        rc.setStatus(ReceivingStatus.POSTED);
        rc.setSourceType(SourceType.ISSUED_CHALLAN);
        rc.setParty(party);
        rc.setSite(site);
        rc.setCreatedBy("admin");

        ReceivingChallanItem rci = new ReceivingChallanItem();
        rci.setReceivingChallan(rc);
        rci.setItem(item1);
        rci.setGoodReturnedQuantity(new BigDecimal("4.00"));
        rci.setDamagedReturnedQuantity(BigDecimal.ZERO);
        rci.setLostQuantity(BigDecimal.ZERO);
        rci.setItemCodeSnapshot(item1.getItemCode());
        rci.setItemNameSnapshot(item1.getItemName());
        rci.setUnitSnapshot(item1.getUnit());
        rci.setPendingQuantitySnapshot(new BigDecimal("10.00"));
        rci.setWeightPerPieceSnapshot(item1.getWeightPerPiece());
        rci.setSequence(1);
        rc.addItem(rci);
        receivingChallans.save(rc);

        // 3. Create Billing Run draft
        BillingRunRequest req = new BillingRunRequest(
                agreement.getId(),
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 25)
        );

        String runStr = mvc.perform(post("/api/v1/billing-runs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        BillingRunResponse run = json.readValue(runStr, BillingRunResponse.class);
        assertNotNull(run.id());
        assertEquals("CALCULATED", run.status());

        // Verify segments
        assertEquals(3, run.segments().size());

        // Standard Item 1: 4 units returned on 20-Jun, so 11 days (10 to 20 excluded is 10 days?
        // Wait, start rule: 10-Jun included. End rule: 20-Jun excluded.
        // Days charged: 10,11,12,13,14,15,16,17,18,19 -> 10 days!
        // Amount: 4 * 5 * 10 = ₹200.
        // Standard Item 1 remaining 6 units: unreturned, so up to periodEnd (25-Jun) included.
        // Days charged: 10 to 25 -> 16 days!
        // Amount: 6 * 5 * 16 = ₹480.
        // Slab-based Item 2: 5 units unreturned (16 days).
        // Days 1-10 @ 4.00 (10 days). Days 11-16 @ 3.00 (6 days).
        // Amount per unit: 10 * 4 + 6 * 3 = 40 + 18 = 58.
        // Amount: 5 * 58 = ₹290.
        // Let's verify the sums.
        BigDecimal expectedRentalSubtotal = new BigDecimal("200.00").add(new BigDecimal("480.00")).add(new BigDecimal("290.00"));
        assertEquals(0, expectedRentalSubtotal.compareTo(run.rentalSubtotal()));

        // Verify operational charges: transport charge of 150 from dispatch should be gathered
        assertEquals(1, run.charges().size());
        assertEquals("ISSUED_CHALLAN_TRANSPORT", run.charges().get(0).sourceType());
        assertEquals(0, new BigDecimal("150.00").compareTo(run.charges().get(0).amount()));
        assertTrue(run.charges().get(0).selected());

        // 4. Update adjustments and finalize
        BillingRunUpdateValuesRequest updateReq = new BillingRunUpdateValuesRequest(
                new BigDecimal("50.00"), // manual adjustment
                "PERCENTAGE",
                new BigDecimal("5.00"), // 5% discount
                List.of(run.charges().get(0).id())
        );

        String updatedStr = mvc.perform(put("/api/v1/billing-runs/" + run.id())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        BillingRunResponse updatedRun = json.readValue(updatedStr, BillingRunResponse.class);
        assertEquals(0, new BigDecimal("50.00").compareTo(updatedRun.manualAdjustmentTotal()));

        // Finalize run
        mvc.perform(post("/api/v1/billing-runs/" + run.id() + "/finalize")
                        .with(csrf()))
                .andExpect(status().isOk());

        // 5. Generate Invoice
        String invStr = mvc.perform(post("/api/v1/invoices/from-billing-run/" + run.id())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        InvoiceResponse invoice = json.readValue(invStr, InvoiceResponse.class);
        assertNotNull(invoice.id());
        assertEquals("DRAFT", invoice.status());

        // Issue invoice -> attaches PDF
        String issuedStr = mvc.perform(post("/api/v1/invoices/" + invoice.id() + "/issue")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        InvoiceResponse issuedInvoice = json.readValue(issuedStr, InvoiceResponse.class);
        assertEquals("ISSUED", issuedInvoice.status());
        assertNotNull(issuedInvoice.generatedPdfAttachmentId());

        // Download PDF check
        mvc.perform(get("/api/v1/invoices/" + invoice.id() + "/pdf"))
                .andExpect(status().isOk());

        // Cancel Invoice -> releases allocations
        InvoiceCancelRequest cancelReq = new InvoiceCancelRequest("Billing error correction");
        mvc.perform(post("/api/v1/invoices/" + invoice.id() + "/cancel")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(cancelReq)))
                .andExpect(status().isOk());

        // Check billing run can be cancelled now that the invoice is cancelled
        mvc.perform(post("/api/v1/billing-runs/" + run.id() + "/cancel")
                        .with(csrf())
                        .param("reason", "Cancelled due to invoice rejection"))
                .andExpect(status().isOk());
    }
}
