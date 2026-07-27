package com.stocksync.masterdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksync.BaseIntegrationTest;
import com.stocksync.file.repository.FileAttachmentRepository;
import com.stocksync.inventory.repository.*;
import com.stocksync.party.repository.*;
import com.stocksync.site.repository.SiteRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WithMockUser(username="admin",roles="ADMIN")
class MasterDataIntegrationTest extends BaseIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json;
    @Autowired FileAttachmentRepository files; @Autowired SiteRepository sites;
    @Autowired JdbcTemplate jdbc;
    @Autowired ItemRepository items; @Autowired ItemCategoryRepository categories;
    @Autowired PartyRepository parties; @Autowired VendorRepository vendors;

    @BeforeEach void clean(){
        jdbc.update("UPDATE stock_import_rows SET posted_stock_transaction_id=NULL");
        jdbc.update("DELETE FROM receiving_challan_items");jdbc.update("DELETE FROM receiving_challans");
        jdbc.update("DELETE FROM issued_challan_items");jdbc.update("DELETE FROM issued_challans");
        jdbc.update("DELETE FROM site_order_items");jdbc.update("DELETE FROM site_orders");jdbc.update("DELETE FROM agreement_items");
        jdbc.update("DELETE FROM agreements");jdbc.update("DELETE FROM quotation_items");jdbc.update("DELETE FROM quotations");
        jdbc.update("DELETE FROM agreement_templates");
        jdbc.update("DELETE FROM stock_transactions");jdbc.update("DELETE FROM purchase_items");jdbc.update("DELETE FROM scrap_items");
        jdbc.update("DELETE FROM stock_adjustment_items");jdbc.update("DELETE FROM purchases");jdbc.update("DELETE FROM scrap_entries");
        jdbc.update("DELETE FROM stock_adjustments");jdbc.update("DELETE FROM stock_balances");
        jdbc.update("DELETE FROM stock_import_location_mappings");jdbc.update("DELETE FROM stock_import_rows");
        jdbc.update("DELETE FROM stock_import_batches");jdbc.update("DELETE FROM item_aliases");
        files.deleteAll();sites.deleteAll();items.deleteAll();categories.deleteAll();vendors.deleteAll();parties.deleteAll();
    }

    @Test void categoryAndItemLifecycle() throws Exception {
        long categoryId=id(mvc.perform(post("/api/v1/categories").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Props\",\"description\":\"Support material\",\"active\":true}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        mvc.perform(post("/api/v1/categories").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"props\",\"active\":true}")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CATEGORY_NAME_ALREADY_EXISTS"));
        mvc.perform(post("/api/v1/items").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"itemCode":"PROP-01","itemName":"Adjustable Prop","categoryId":%d,"unit":"PCS",
                     "weightPerPiece":12.5,"purchaseValue":1800,"lossRate":2200,"scrapValue":500,
                     "minimumStock":10,"active":true}
                    """.formatted(categoryId))).andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCode").value("PROP-01"))
                .andExpect(jsonPath("$.categoryName").value("Props"));
        mvc.perform(get("/api/v1/items").param("search","adjustable"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test void viewerCanReadButCannotWriteMasterData() throws Exception {
        mvc.perform(get("/api/v1/categories")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("viewer").roles("VIEWER")))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/categories").with(csrf())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("viewer").roles("VIEWER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Blocked\",\"active\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test void partyVendorAndSiteRules() throws Exception {
        long partyId=id(mvc.perform(post("/api/v1/parties").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"legalName\":\"Build Right Pvt Ltd\",\"tradeName\":\"Build Right\",\"gstin\":\"27ABCDE1234F1Z5\","
                        + "\"pan\":\"ABCDE1234F\",\"active\":true}")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        mvc.perform(post("/api/v1/vendors").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Steel Supply Co\",\"gstin\":\"27AAAAA1111A1Z1\",\"active\":true}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/sites").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"partyId":%d,"siteName":"Tower A","siteCode":"TOWER-A","startDate":"2026-07-01",
                     "expectedEndDate":"2026-06-01","status":"ACTIVE","defaulter":false}
                    """.formatted(partyId))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SITE_DATES"));
        mvc.perform(post("/api/v1/sites").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"partyId":%d,"siteName":"Tower A","siteCode":"TOWER-A","startDate":"2026-07-01",
                     "expectedEndDate":"2027-06-01","status":"ACTIVE","defaulter":false}
                    """.formatted(partyId))).andExpect(status().isOk());
    }

    @Test void documentUploadValidatesTypeAndLinksMetadata() throws Exception {
        long partyId=id(mvc.perform(post("/api/v1/parties").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"legalName\":\"Document Party\",\"active\":true}")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        MockMultipartFile file=new MockMultipartFile("file","gst-certificate.pdf","application/pdf","PDF".getBytes());
        mvc.perform(multipart("/api/v1/files").file(file).with(csrf()).param("entityType","PARTY")
                .param("entityId",String.valueOf(partyId)).param("documentType","GST_CERTIFICATE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.originalFilename").value("gst-certificate.pdf"));
        mvc.perform(get("/api/v1/files").param("entityType","PARTY").param("entityId",String.valueOf(partyId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].documentType").value("GST_CERTIFICATE"));
    }
    private long id(String body)throws Exception{JsonNode node=json.readTree(body);return node.get("id").asLong();}
}
