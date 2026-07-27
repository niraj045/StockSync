package com.stocksync.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksync.BaseIntegrationTest;
import com.stocksync.agreement.entity.*;
import com.stocksync.agreement.repository.AgreementRepository;
import com.stocksync.billing.entity.*;
import com.stocksync.billing.repository.BillingRunRepository;
import com.stocksync.billing.repository.InvoiceRepository;
import com.stocksync.party.entity.Party;
import com.stocksync.party.repository.PartyRepository;
import com.stocksync.payment.dto.PaymentDtos.*;
import com.stocksync.payment.dto.SecurityDepositDtos.*;
import com.stocksync.quotation.entity.RentalType;
import com.stocksync.site.entity.Site;
import com.stocksync.site.entity.SiteStatus;
import com.stocksync.site.repository.SiteRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = {"ADMIN", "ACCOUNTS"})
class Phase8PaymentIntegrationTest extends BaseIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PartyRepository parties;
    @Autowired SiteRepository sites;
    @Autowired AgreementRepository agreements;
    @Autowired BillingRunRepository billingRuns;
    @Autowired InvoiceRepository invoices;

    Party party;
    Site site;
    Agreement agreement;
    Invoice invoice1;
    Invoice invoice2;

    @BeforeEach
    void setup() {
        clean();
        party = new Party();
        party.setLegalName("Phase Eight Infra");
        party.setAddress("Ahmedabad");
        party.setState("GUJARAT");
        party.setCreatedBy("admin");
        party = parties.save(party);

        site = new Site();
        site.setParty(party);
        site.setSiteName("Phase Eight Site");
        site.setSiteCode("P8");
        site.setAddress("Ahmedabad");
        site.setStatus(SiteStatus.ACTIVE);
        site.setCreatedBy("admin");
        site = sites.save(site);

        agreement = new Agreement();
        agreement.setAgreementNumber("AGR-P8");
        agreement.setParty(party);
        agreement.setSite(site);
        agreement.setAgreementDate(LocalDate.of(2026, 7, 1));
        agreement.setEffectiveDate(LocalDate.of(2026, 7, 1));
        agreement.setRentalType(RentalType.PER_PIECE_PER_DAY);
        agreement.setBillingCycle(BillingCycle.MONTHLY);
        agreement.setStatus(AgreementStatus.ACTIVE);
        agreement.setPartyLegalNameSnapshot(party.getLegalName());
        agreement.setPartyAddressSnapshot(party.getAddress());
        agreement.setPartyStateSnapshot(party.getState());
        agreement.setSiteNameSnapshot(site.getSiteName());
        agreement.setSiteCodeSnapshot(site.getSiteCode());
        agreement.setSiteAddressSnapshot(site.getAddress());
        agreement.setSecurityDeposit(new BigDecimal("500.00"));
        agreement.setCreatedBy("admin");
        agreement.setUpdatedBy("admin");
        agreement = agreements.save(agreement);

        invoice1 = invoice("INV-P8-1", "BR-P8-1", new BigDecimal("1000.00"));
        invoice2 = invoice("INV-P8-2", "BR-P8-2", new BigDecimal("700.00"));
    }

    @AfterEach
    void cleanup() { clean(); }

    @Test
    void paymentsDepositsOutstandingAndReceiptPdfWork() throws Exception {
        PaymentRequest payment = new PaymentRequest(
                party.getId(), null, LocalDate.of(2026, 7, 10), "NEFT", "UTR-1", null, null, null,
                new BigDecimal("900.00"), new BigDecimal("100.00"), "Mixed settlement",
                List.of(new AllocationRequest(invoice1.getId(), new BigDecimal("500.00"), new BigDecimal("100.00"))), null);

        String draftJson = mvc.perform(post("/api/v1/payments").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payment)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long paymentId = json.readTree(draftJson).get("id").asLong();

        String postedJson = mvc.perform(post("/api/v1/payments/" + paymentId + "/post").with(csrf()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode posted = json.readTree(postedJson);
        assertEquals("POSTED", posted.get("status").asText());
        assertEquals(400, posted.get("unallocatedAmount").decimalValue().intValue());
        assertMoney("400.00", invoice1.getId(), "outstanding_amount");
        assertMoney("500.00", invoice1.getId(), "cash_allocated_total");
        assertMoney("100.00", invoice1.getId(), "tds_allocated_total");

        AllocateRequest later = new AllocateRequest(List.of(new AllocationRequest(invoice2.getId(), new BigDecimal("300.00"), BigDecimal.ZERO)));
        mvc.perform(post("/api/v1/payments/" + paymentId + "/allocate").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(later)))
                .andExpect(status().isOk());
        assertMoney("400.00", invoice2.getId(), "outstanding_amount");

        PaymentRequest excessive = new PaymentRequest(
                party.getId(), null, LocalDate.of(2026, 7, 11), "CASH", null, null, null, null,
                new BigDecimal("50.00"), BigDecimal.ZERO, null,
                List.of(new AllocationRequest(invoice2.getId(), new BigDecimal("500.00"), BigDecimal.ZERO)), null);
        String overDraft = mvc.perform(post("/api/v1/payments").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(excessive)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/v1/payments/" + json.readTree(overDraft).get("id").asLong() + "/post").with(csrf()))
                .andExpect(status().isBadRequest());

        TdsDetailsRequest tds = new TdsDetailsRequest(LocalDate.of(2026, 7, 10), "194C", "CERT-1", LocalDate.of(2026, 7, 20), null);
        mvc.perform(put("/api/v1/payments/" + paymentId + "/tds-details").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(tds)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/payments/" + paymentId + "/tds/verify").with(csrf())).andExpect(status().isOk());

        mvc.perform(post("/api/v1/security-deposits/receipt").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new DepositReceiptRequest(agreement.getId(), LocalDate.of(2026, 7, 12), new BigDecimal("500.00"), "CASH", null, "Deposit received"))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/security-deposits/adjust-to-invoice").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new DepositAdjustmentRequest(agreement.getId(), invoice1.getId(), LocalDate.of(2026, 7, 13), new BigDecimal("200.00"), "Explicit adjustment"))))
                .andExpect(status().isOk());
        assertMoney("200.00", invoice1.getId(), "outstanding_amount");
        mvc.perform(post("/api/v1/security-deposits/refund").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new DepositRefundRequest(agreement.getId(), LocalDate.of(2026, 7, 14), new BigDecimal("400.00"), "CASH", null, "Too much"))))
                .andExpect(status().isBadRequest());

        JsonNode outstanding = json.readTree(mvc.perform(get("/api/v1/outstanding/invoices/" + invoice1.getId()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertEquals("PARTIALLY_PAID", outstanding.get("paymentStatus").asText());
        assertEquals(500, outstanding.get("cashAllocated").decimalValue().intValue());
        assertEquals(100, outstanding.get("tdsAllocated").decimalValue().intValue());
        assertEquals(200, outstanding.get("depositAdjusted").decimalValue().intValue());

        mvc.perform(get("/api/v1/payments/" + paymentId + "/receipt")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/payments/" + paymentId + "/reverse").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ReasonRequest("Wrong customer reference"))))
                .andExpect(status().isOk());
        assertMoney("800.00", invoice1.getId(), "outstanding_amount");
        assertMoney("700.00", invoice2.getId(), "outstanding_amount");
    }

    @Test
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    void viewerCannotPostPayments() throws Exception {
        mvc.perform(post("/api/v1/payments/1/post").with(csrf())).andExpect(status().isForbidden());
    }

    private Invoice invoice(String invoiceNumber, String runNumber, BigDecimal amount) {
        BillingRun run = new BillingRun();
        run.setBillingRunNumber(runNumber);
        run.setAgreement(agreement);
        run.setParty(party);
        run.setSite(site);
        run.setPeriodStart(LocalDate.of(2026, 7, 1));
        run.setPeriodEnd(LocalDate.of(2026, 7, 31));
        run.setStatus(BillingRunStatus.FINALIZED);
        run.setGrandTotal(amount);
        run.setCreatedBy("admin");
        run.setUpdatedBy("admin");
        run = billingRuns.save(run);

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setBillingRun(run);
        invoice.setAgreement(agreement);
        invoice.setParty(party);
        invoice.setSite(site);
        invoice.setInvoiceDate(LocalDate.of(2026, 7, 5));
        invoice.setDueDate(LocalDate.of(2026, 8, 4));
        invoice.setPeriodStart(LocalDate.of(2026, 7, 1));
        invoice.setPeriodEnd(LocalDate.of(2026, 7, 31));
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setCompanyNameSnapshot("StockSync");
        invoice.setCompanyAddressSnapshot("GIDC");
        invoice.setCompanyGstinSnapshot("24AAAAA1111A1Z1");
        invoice.setPartyLegalNameSnapshot(party.getLegalName());
        invoice.setPartyAddressSnapshot(party.getAddress());
        invoice.setPartyStateSnapshot(party.getState());
        invoice.setSiteNameSnapshot(site.getSiteName());
        invoice.setSiteCodeSnapshot(site.getSiteCode());
        invoice.setSiteAddressSnapshot(site.getAddress());
        invoice.setAgreementNumberSnapshot(agreement.getAgreementNumber());
        invoice.setGrandTotal(amount);
        invoice.setOutstandingAmount(amount);
        invoice.setCreatedBy("admin");
        invoice.setUpdatedBy("admin");
        return invoices.save(invoice);
    }

    private void assertMoney(String expected, Long invoiceId, String column) {
        BigDecimal actual = jdbc.queryForObject("SELECT " + column + " FROM invoices WHERE id = ?", BigDecimal.class, invoiceId);
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private void clean() {
        jdbc.update("SET FOREIGN_KEY_CHECKS = 0");
        jdbc.update("DELETE FROM deposit_invoice_allocations");
        jdbc.update("DELETE FROM security_deposit_transactions");
        jdbc.update("DELETE FROM tds_details");
        jdbc.update("DELETE FROM payment_allocations");
        jdbc.update("DELETE FROM payment_receipts");
        jdbc.update("DELETE FROM invoice_items");
        jdbc.update("DELETE FROM invoices");
        jdbc.update("DELETE FROM billing_run_segments");
        jdbc.update("DELETE FROM billing_run_charges");
        jdbc.update("DELETE FROM billing_source_allocations");
        jdbc.update("DELETE FROM billing_runs");
        jdbc.update("DELETE FROM agreement_item_slabs");
        jdbc.update("DELETE FROM agreement_items");
        jdbc.update("DELETE FROM agreements");
        jdbc.update("DELETE FROM sites");
        jdbc.update("DELETE FROM parties");
        jdbc.update("SET FOREIGN_KEY_CHECKS = 1");
    }
}
