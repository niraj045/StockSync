package com.stocksync.quotation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksync.BaseIntegrationTest;
import com.stocksync.inventory.entity.Item;
import com.stocksync.inventory.entity.ItemCategory;
import com.stocksync.inventory.repository.ItemCategoryRepository;
import com.stocksync.inventory.repository.ItemRepository;
import com.stocksync.party.entity.Party;
import com.stocksync.party.repository.PartyRepository;
import com.stocksync.quotation.entity.QuotationTemplate;
import com.stocksync.quotation.repository.QuotationTemplateRepository;
import com.stocksync.site.entity.Site;
import com.stocksync.site.entity.SiteStatus;
import com.stocksync.site.repository.SiteRepository;
import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class QuotationPdfIntegrationTest extends BaseIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PartyRepository parties;
    @Autowired SiteRepository sites;
    @Autowired ItemCategoryRepository categories;
    @Autowired ItemRepository items;
    @Autowired QuotationTemplateRepository templates;
    @Autowired QuotationPdfService pdfService;
    @Autowired QuotationService quotationService;

    private Party party;
    private Site site;
    private Item item;
    private QuotationTemplate template;

    @BeforeEach
    void setup() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        party = new Party();
        party.setLegalName("Snapshot Party " + suffix);
        party.setActive(true);
        party = parties.save(party);

        site = new Site();
        site.setParty(party);
        site.setSiteName("Snapshot Site " + suffix);
        site.setSiteCode("PDF-" + suffix);
        site.setStatus(SiteStatus.ACTIVE);
        site = sites.save(site);

        ItemCategory category = new ItemCategory();
        category.setName("PDF Category " + suffix);
        category.setActive(true);
        category = categories.save(category);

        item = new Item();
        item.setItemCode("PDF-" + suffix);
        item.setItemName("Snapshot Plate " + suffix);
        item.setCategory(category);
        item.setSize("1250 x 500");
        item.setUnit("PCS");
        item.setMinimumStock(BigDecimal.ZERO);
        item.setActive(true);
        item = items.save(item);

        template = new QuotationTemplate();
        template.setTemplateCode("PDF-" + suffix);
        template.setName("PDF Standard");
        template.setCompanyName("StockSync Test Company");
        template.setHeaderText("Commercial quotation");
        template.setActive(true);
        template = templates.save(template);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "OPERATIONS", "ACCOUNTS", "VIEWER"})
    void canonicalRolesCanDownloadPdf(String role) throws Exception {
        long quotationId = createQuotation("ADMIN");
        mvc.perform(get("/api/v1/quotations/{id}/pdf", quotationId).with(user(role.toLowerCase()).roles(role)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.matchesPattern("attachment; filename=\"quotation-QT-[A-Za-z0-9.-]+\\.pdf\"")));
    }

    @Test
    void unauthenticatedAndUnsupportedUsersReceiveJsonErrors() throws Exception {
        long quotationId = createQuotation("ADMIN");
        mvc.perform(get("/api/v1/quotations/{id}/pdf", quotationId))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("SESSION_EXPIRED"));
        mvc.perform(get("/api/v1/quotations/{id}/pdf", quotationId).with(user("unsupported").roles("SUPPORT")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void pdfUsesStoredCommercialAndMasterSnapshots() throws Exception {
        String originalParty = party.getLegalName();
        String originalSite = site.getSiteName();
        String originalItem = item.getItemName();
        long quotationId = createQuotation("ADMIN");
        approve(quotationId);

        party.setLegalName("Changed Party Master");
        parties.saveAndFlush(party);
        site.setSiteName("Changed Site Master");
        sites.saveAndFlush(site);
        item.setItemName("Changed Item Master");
        item.setSize("CHANGED SIZE");
        item.setUnit("KG");
        items.saveAndFlush(item);

        byte[] bytes = mvc.perform(get("/api/v1/quotations/{id}/pdf", quotationId).with(user("viewer").roles("VIEWER")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        String text;
        try (PDDocument document = PDDocument.load(bytes)) {
            text = new PDFTextStripper().getText(document);
        }
        assertThat(text).contains("QT/", originalParty, originalSite, originalItem, "1250 x 500", "PCS",
                "25.00", "250.00", "Transport", "10.00", "Grand total", "306.80");
        assertThat(text).doesNotContain("Changed Party Master", "Changed Site Master", "Changed Item Master", "CHANGED SIZE");
    }

    @Test
    void approvedAndCancelledQuotationsRemainDownloadable() throws Exception {
        long approved = createQuotation("ADMIN");
        approve(approved);
        long cancelled = createQuotation("ADMIN");
        mvc.perform(post("/api/v1/quotations/{id}/cancel", cancelled).with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Client cancelled\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/quotations/{id}/pdf", approved).with(user("accounts").roles("ACCOUNTS"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/quotations/{id}/pdf", cancelled).with(user("viewer").roles("VIEWER"))).andExpect(status().isOk());
    }

    @Test
    void inMemoryPdfPathLeavesNoTemporaryFilesAfterSuccessOrFailure() throws Exception {
        Set<String> before = quotationTempFiles();
        long quotationId = createQuotation("ADMIN");
        pdfService.generate(quotationId);
        assertThat(quotationTempFiles()).isEqualTo(before);
        assertThatThrownBy(() -> pdfService.render(quotationService.get(quotationId), "<broken"))
                .isInstanceOf(RuntimeException.class);
        assertThat(quotationTempFiles()).isEqualTo(before);
    }

    @Test
    void quotationLifecycleDoesNotChangeStockLedgerOrBalances() throws Exception {
        long transactionCount = jdbc.queryForObject("SELECT COUNT(*) FROM stock_transactions", Long.class);
        BigDecimal available = jdbc.queryForObject("SELECT COALESCE(SUM(available_quantity),0) FROM stock_balances", BigDecimal.class);
        long quotationId = createQuotation("OPERATIONS");
        approve(quotationId);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_transactions", Long.class)).isEqualTo(transactionCount);
        assertThat(jdbc.queryForObject("SELECT COALESCE(SUM(available_quantity),0) FROM stock_balances", BigDecimal.class))
                .isEqualByComparingTo(available);
    }

    private long createQuotation(String role) throws Exception {
        String body = """
                {"quotationTemplateId":%d,"partyId":%d,"siteId":%d,"quotationDate":"2030-07-26","validUntil":"2030-08-26",
                "rentalType":"PER_PIECE_PER_DAY","discountType":"NONE","discountValue":0,
                "cgstRate":9,"sgstRate":9,"igstRate":0,"transportCharge":10,"loadingCharge":0,"unloadingCharge":0,
                "otherCharge":0,"roundOff":0,"securityDeposit":100,"terms":"Net 30","notes":"Snapshot test",
                "items":[{"itemId":%d,"quantity":10,"rate":25,"rentalType":"PER_PIECE_PER_DAY"}]}
                """.formatted(template.getId(), party.getId(), site.getId(), item.getId());
        String response = mvc.perform(post("/api/v1/quotations").with(user(role.toLowerCase()).roles(role)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }

    private void approve(long id) throws Exception {
        mvc.perform(post("/api/v1/quotations/{id}/send", id).with(user("operations").roles("OPERATIONS")).with(csrf()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/quotations/{id}/approve", id).with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk());
    }

    private Set<String> quotationTempFiles() {
        File directory = new File(System.getProperty("java.io.tmpdir"));
        File[] files = directory.listFiles((ignored, name) -> name.toLowerCase().startsWith("quotation-"));
        return files == null ? Set.of() : Arrays.stream(files).map(File::getAbsolutePath).collect(Collectors.toSet());
    }
}
