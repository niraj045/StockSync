package com.stocksync.phase4;

import com.fasterxml.jackson.databind.*;
import com.stocksync.BaseIntegrationTest;
import com.stocksync.inventory.entity.*;
import com.stocksync.inventory.repository.*;
import com.stocksync.party.entity.Party;
import com.stocksync.party.repository.PartyRepository;
import com.stocksync.site.entity.*;
import com.stocksync.site.repository.SiteRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WithMockUser(username="operations",roles="OPERATIONS")
class Phase4IntegrationTest extends BaseIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc;
    @Autowired PartyRepository parties; @Autowired SiteRepository sites; @Autowired ItemCategoryRepository categories; @Autowired ItemRepository items;
    private Party party; private Site site; private Item plate; private Item prop;

    @BeforeEach void setup(){
        jdbc.update("DELETE FROM site_order_items");jdbc.update("DELETE FROM site_orders");jdbc.update("DELETE FROM agreement_items");
        jdbc.update("DELETE FROM agreements");jdbc.update("DELETE FROM quotation_items");jdbc.update("DELETE FROM quotations");
        jdbc.update("DELETE FROM agreement_templates");jdbc.update("DELETE FROM stock_transactions");jdbc.update("DELETE FROM purchase_items");
        jdbc.update("DELETE FROM scrap_items");jdbc.update("DELETE FROM stock_adjustment_items");jdbc.update("DELETE FROM purchases");
        jdbc.update("DELETE FROM scrap_entries");jdbc.update("DELETE FROM stock_adjustments");jdbc.update("DELETE FROM stock_balances");
        jdbc.update("DELETE FROM file_attachments");sites.deleteAll();items.deleteAll();categories.deleteAll();parties.deleteAll();
        party=new Party();party.setLegalName("Phase Four Construction");party.setActive(true);party.setCreatedBy("test");party.setUpdatedBy("test");party=parties.save(party);
        site=new Site();site.setParty(party);site.setSiteName("Phase Four Site");site.setSiteCode("P4-SITE");site.setStatus(SiteStatus.ACTIVE);
        site.setCreatedBy("test");site.setUpdatedBy("test");site=sites.save(site);
        ItemCategory category=new ItemCategory();category.setName("Phase Four Materials");category.setActive(true);category.setCreatedBy("test");category.setUpdatedBy("test");
        category=categories.save(category);plate=item(category,"P4-PLATE","Plate");prop=item(category,"P4-PROP","Prop");
    }

    @Test void quotationCalculatesTotalsAndEnforcesLifecycle() throws Exception {
        long id=createQuotation();
        mvc.perform(get("/api/v1/quotations/{id}",id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.subtotal").value(2150.0)).andExpect(jsonPath("$.taxAmount").value(387.0))
            .andExpect(jsonPath("$.grandTotal").value(2537.0)).andExpect(jsonPath("$.status").value("DRAFT"));
        mvc.perform(post("/api/v1/quotations/{id}/send",id).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SENT"));
        mvc.perform(put("/api/v1/quotations/{id}",id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(quotationBody()))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("QUOTATION_IMMUTABLE"));
        mvc.perform(post("/api/v1/quotations/{id}/approve",id).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test void approvedQuotationConvertsGeneratesAndActivatesAgreement() throws Exception {
        long quotationId=approveQuotation();
        String converted=mvc.perform(post("/api/v1/quotations/{id}/convert",quotationId).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"effectiveDate\":\"2030-01-01\",\"expiryDate\":\"2030-12-31\",\"securityDeposit\":5000,\"notes\":\"Converted\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DRAFT")).andExpect(jsonPath("$.items.length()").value(2))
            .andReturn().getResponse().getContentAsString();
        long agreementId=json.readTree(converted).get("id").asLong();
        mvc.perform(post("/api/v1/agreements/{id}/generate",agreementId).with(csrf())).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("GENERATED")).andExpect(jsonPath("$.generated").value(true));
        mvc.perform(get("/api/v1/agreements/{id}/document",agreementId)).andExpect(status().isOk())
            .andExpect(header().string("Content-Type","application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        mvc.perform(post("/api/v1/agreements/{id}/activate",agreementId).with(csrf())).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
        mvc.perform(post("/api/v1/quotations/{id}/convert",quotationId).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"effectiveDate\":\"2030-01-01\",\"securityDeposit\":0}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("QUOTATION_ALREADY_CONVERTED"));
    }

    @Test void confirmedOrdersRespectAgreementAllocationAndExposeRemainingQuantity() throws Exception {
        long agreementId=activeAgreement();
        long first=createOrder(agreementId,"60","10");
        mvc.perform(post("/api/v1/orders/{id}/confirm",first).with(csrf())).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED")).andExpect(jsonPath("$.items[0].remainingQuantity").value(60.0));
        long second=createOrder(agreementId,"50","5");
        mvc.perform(post("/api/v1/orders/{id}/confirm",second).with(csrf())).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("AGREEMENT_QUANTITY_EXCEEDED"));
        mvc.perform(get("/api/v1/orders/{id}",second)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test @WithMockUser(username="viewer",roles="VIEWER")
    void viewerCannotCreateQuotationOrUploadTemplate() throws Exception {
        mvc.perform(post("/api/v1/quotations").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(quotationBody())).andExpect(status().isForbidden());
        MockMultipartFile file=new MockMultipartFile("file","template.pdf","application/pdf","%PDF-test".getBytes());
        mvc.perform(multipart("/api/v1/agreement-templates").file(file).param("name","Viewer Template").with(csrf())).andExpect(status().isForbidden());
    }

    private Item item(ItemCategory category,String code,String name){Item i=new Item();i.setItemCode(code);i.setItemName(name);i.setCategory(category);i.setUnit("PCS");
        i.setMinimumStock(BigDecimal.ZERO);i.setActive(true);i.setCreatedBy("test");i.setUpdatedBy("test");return items.save(i);}
    private long createQuotation()throws Exception{String result=mvc.perform(post("/api/v1/quotations").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(quotationBody()))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();return json.readTree(result).get("id").asLong();}
    private long approveQuotation()throws Exception{long id=createQuotation();mvc.perform(post("/api/v1/quotations/{id}/send",id).with(csrf())).andExpect(status().isOk());
        mvc.perform(post("/api/v1/quotations/{id}/approve",id).with(csrf())).andExpect(status().isOk());return id;}
    private long activeAgreement()throws Exception{long q=approveQuotation();String body=mvc.perform(post("/api/v1/quotations/{id}/convert",q).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content("{\"effectiveDate\":\"2030-01-01\",\"expiryDate\":\"2030-12-31\",\"securityDeposit\":0}"))
        .andReturn().getResponse().getContentAsString();long id=json.readTree(body).get("id").asLong();
        mvc.perform(post("/api/v1/agreements/{id}/generate",id).with(csrf())).andExpect(status().isOk());
        mvc.perform(post("/api/v1/agreements/{id}/activate",id).with(csrf())).andExpect(status().isOk());return id;}
    private long createOrder(long agreementId,String plates,String props)throws Exception{String result=mvc.perform(post("/api/v1/orders").with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content("""
            {"agreementId":%d,"orderDate":"2030-01-02","notes":"Dispatch requirement","items":[
            {"itemId":%d,"orderedQuantity":%s},{"itemId":%d,"orderedQuantity":%s}]}
            """.formatted(agreementId,plate.getId(),plates,prop.getId(),props))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(result).get("id").asLong();}
    private String quotationBody(){return """
        {"partyId":%d,"siteId":%d,"quotationDate":"2030-01-01","validUntil":"2030-02-01","rentalType":"PER_PIECE_PER_DAY",
        "transportCharge":100,"loadingCharge":25,"unloadingCharge":25,"taxRate":18,"terms":"Net 30","notes":"Phase 4 test",
        "items":[{"itemId":%d,"quantity":100,"unitRate":20,"rentalRate":1.5},{"itemId":%d,"quantity":20,"unitRate":0,"rentalRate":2}]}
        """.formatted(party.getId(),site.getId(),plate.getId(),prop.getId());}
}
