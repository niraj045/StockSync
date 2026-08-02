package com.stocksync.phase6a;

import com.fasterxml.jackson.databind.*;
import com.stocksync.BaseIntegrationTest;
import com.stocksync.challan.dto.*;
import com.stocksync.challan.repository.IssuedChallanRepository;
import com.stocksync.inventory.entity.*;
import com.stocksync.inventory.repository.*;
import com.stocksync.inventory.service.InventoryService;
import com.stocksync.inventory.dto.*;
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
class Phase6AIntegrationTest extends BaseIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PartyRepository parties;
    @Autowired SiteRepository sites;
    @Autowired ItemCategoryRepository categories;
    @Autowired ItemRepository items;
    @Autowired VendorRepository vendors;
    @Autowired StockBalanceRepository balances;
    @Autowired SiteStockBalanceRepository siteBalances;
    @Autowired IssuedChallanRepository challans;
    @Autowired InventoryService inventory;

    private Party party;
    private Site site;
    private Item plate;
    private Item prop;
    private Vendor vendor;
    private long quotationTemplateId;

    @BeforeEach
    void setup() {
        jdbc.update("UPDATE stock_import_rows SET posted_stock_transaction_id=NULL");
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
        jdbc.update("DELETE FROM site_stock_balances");
        jdbc.update("DELETE FROM stock_balances");
        jdbc.update("DELETE FROM stock_import_location_mappings");
        jdbc.update("DELETE FROM stock_import_rows");
        jdbc.update("DELETE FROM stock_import_batches");
        jdbc.update("DELETE FROM item_aliases");
        jdbc.update("DELETE FROM file_attachments");

        sites.deleteAll();
        items.deleteAll();
        categories.deleteAll();
        parties.deleteAll();
        vendors.deleteAll();

        party = new Party();
        party.setLegalName("Phase Six Construction");
        party.setActive(true);
        party.setCreatedBy("test");
        party.setUpdatedBy("test");
        party = parties.save(party);

        site = new Site();
        site.setParty(party);
        site.setSiteName("Phase Six Site");
        site.setSiteCode("P6-SITE");
        site.setStatus(SiteStatus.ACTIVE);
        site.setCreatedBy("test");
        site.setUpdatedBy("test");
        site = sites.save(site);

        vendor = new Vendor();
        vendor.setName("Phase Six Supplier");
        vendor.setActive(true);
        vendor.setCreatedBy("test");
        vendor.setUpdatedBy("test");
        vendor = vendors.save(vendor);

        ItemCategory category = new ItemCategory();
        category.setName("Phase Six Materials");
        category.setActive(true);
        category.setCreatedBy("test");
        category.setUpdatedBy("test");
        category = categories.save(category);

        plate = item(category, "P6-PLATE", "Plate");
        prop = item(category, "P6-PROP", "Prop");

        jdbc.update("INSERT INTO quotation_templates(template_code,name,active,version,created_at,created_by,updated_at,updated_by) VALUES('P6','Phase 6 Compatibility',true,0,NOW(6),'test',NOW(6),'test')");
        quotationTemplateId = jdbc.queryForObject("SELECT id FROM quotation_templates WHERE template_code='P6'", Long.class);
    }

    @Test
    void issuedChallanLifecycleEnforcesBalancesAndOrderQuantities() throws Exception {
        long agreementId = activeAgreement();
        long orderId = createOrder(agreementId, "60", "10");

        // 1. Confirm the order
        mvc.perform(post("/api/v1/orders/{id}/confirm", orderId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // 2. Try to dispatch without godown stock -> should fail
        String challanJson = """
            {"siteOrderId":%d,"dispatchDate":"2030-01-05","vehicleNumber":"MH-12-AB-1234","driverName":"Rajesh","notes":"First load",
            "items":[{"itemId":%d,"quantity":40}]}
            """.formatted(orderId, plate.getId());

        mvc.perform(post("/api/v1/challans/issued").contentType(MediaType.APPLICATION_JSON).content(challanJson).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

        // 3. Purchase godown stock (100 plates, 50 props)
        purchaseStock();

        // 4. Dispatch partial order (40 plates, 5 props)
        String partialChallanJson = """
            {"siteOrderId":%d,"dispatchDate":"2030-01-05","vehicleNumber":"MH-12-AB-1234","driverName":"Rajesh","notes":"First load",
            "items":[{"itemId":%d,"quantity":40},{"itemId":%d,"quantity":5}]}
            """.formatted(orderId, plate.getId(), prop.getId());

        String result = mvc.perform(post("/api/v1/challans/issued").contentType(MediaType.APPLICATION_JSON).content(partialChallanJson).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challanNumber").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        long challanId = json.readTree(result).get("id").asLong();

        // Check stock balances
        // Godown available plates: 100 - 40 = 60
        // Global issued plates: 40
        // Site pending plates: 40
        StockBalance plateBal = balances.findById(plate.getId()).orElseThrow();
        Assertions.assertEquals(0, BigDecimal.valueOf(60.0).compareTo(plateBal.getAvailableQuantity()));
        Assertions.assertEquals(0, BigDecimal.valueOf(40.0).compareTo(plateBal.getIssuedQuantity()));

        SiteStockBalance sitePlateBal = siteBalances.findBySiteIdAndItemId(site.getId(), plate.getId()).orElseThrow();
        Assertions.assertEquals(0, BigDecimal.valueOf(40.0).compareTo(sitePlateBal.getPendingQuantity()));

        // Check order status -> PARTIALLY_FULFILLED
        mvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIALLY_FULFILLED"))
                .andExpect(jsonPath("$.items[0].issuedQuantity").value(40.0))
                .andExpect(jsonPath("$.items[0].remainingQuantity").value(20.0));

        // 5. Try to dispatch more than remaining order -> should fail
        String overDispatchJson = """
            {"siteOrderId":%d,"dispatchDate":"2030-01-06","items":[{"itemId":%d,"quantity":30}]}
            """.formatted(orderId, plate.getId());

        mvc.perform(post("/api/v1/challans/issued").contentType(MediaType.APPLICATION_JSON).content(overDispatchJson).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DISPATCH_QUANTITY_EXCEEDED"));

        // 6. Complete remaining dispatch (20 plates, 5 props)
        String completeChallanJson = """
            {"siteOrderId":%d,"dispatchDate":"2030-01-06","items":[{"itemId":%d,"quantity":20},{"itemId":%d,"quantity":5}]}
            """.formatted(orderId, plate.getId(), prop.getId());

        mvc.perform(post("/api/v1/challans/issued").contentType(MediaType.APPLICATION_JSON).content(completeChallanJson).with(csrf()))
                .andExpect(status().isOk());

        // Check stock balances after full dispatch
        plateBal = balances.findById(plate.getId()).orElseThrow();
        Assertions.assertEquals(0, BigDecimal.valueOf(40.0).compareTo(plateBal.getAvailableQuantity()));
        Assertions.assertEquals(0, BigDecimal.valueOf(60.0).compareTo(plateBal.getIssuedQuantity()));

        sitePlateBal = siteBalances.findBySiteIdAndItemId(site.getId(), plate.getId()).orElseThrow();
        Assertions.assertEquals(0, BigDecimal.valueOf(60.0).compareTo(sitePlateBal.getPendingQuantity()));

        // Check order status -> FULFILLED
        mvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FULFILLED"))
                .andExpect(jsonPath("$.items[0].issuedQuantity").value(60.0))
                .andExpect(jsonPath("$.items[0].remainingQuantity").value(0.0));

        // 7. Verify PDF download endpoint works
        mvc.perform(get("/api/v1/challans/issued/{id}/pdf", challanId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    private Item item(ItemCategory category, String code, String name) {
        Item i = new Item();
        i.setItemCode(code);
        i.setItemName(name);
        i.setCategory(category);
        i.setUnit("PCS");
        i.setMinimumStock(BigDecimal.ZERO);
        i.setActive(true);
        i.setCreatedBy("test");
        i.setUpdatedBy("test");
        return items.save(i);
    }

    private void purchaseStock() {
        StockLineRequest l1 = new StockLineRequest(plate.getId(), BigDecimal.valueOf(100), BigDecimal.TEN);
        StockLineRequest l2 = new StockLineRequest(prop.getId(), BigDecimal.valueOf(50), BigDecimal.valueOf(15));
        inventory.purchase("KEY-" + System.currentTimeMillis(), new PurchaseRequest(vendor.getId(), LocalDate.now(), "Supply", List.of(l1, l2)), null);
    }

    private long createQuotation() throws Exception {
        String result = mvc.perform(post("/api/v1/quotations").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(quotationBody()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(result).get("id").asLong();
    }

    private long approveQuotation() throws Exception {
        long id = createQuotation();
        mvc.perform(post("/api/v1/quotations/{id}/send", id).with(csrf())).andExpect(status().isOk());
        mvc.perform(post("/api/v1/quotations/{id}/approve", id).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isOk());
        return id;
    }

    private long activeAgreement() throws Exception {
        long q = approveQuotation();
        String body = mvc.perform(post("/api/v1/agreements/from-quotation/{id}", q).with(csrf()))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(body).get("id").asLong();
        mvc.perform(post("/api/v1/agreements/{id}/generate-document", id).with(csrf())).andExpect(status().isOk());
        mvc.perform(post("/api/v1/agreements/{id}/ready-for-review", id).with(csrf())).andExpect(status().isOk());
        mvc.perform(post("/api/v1/agreements/{id}/activate", id).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isOk());
        return id;
    }

    private long createOrder(long agreementId, String plates, String props) throws Exception {
        String result = mvc.perform(post("/api/v1/orders").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"agreementId":%d,"orderDate":"2030-01-02","notes":"Dispatch requirement","items":[
                    {"itemId":%d,"orderedQuantity":%s},{"itemId":%d,"orderedQuantity":%s}]}
                    """.formatted(agreementId, plate.getId(), plates, prop.getId(), props))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(result).get("id").asLong();
    }

    private String quotationBody() {
        return """
            {"quotationTemplateId":%d,"partyId":%d,"siteId":%d,"quotationDate":"2030-01-01","validUntil":"2030-02-01","rentalType":"PER_PIECE_PER_DAY",
            "discountType":"NONE","discountValue":0,"transportCharge":100,"loadingCharge":25,"unloadingCharge":25,"otherCharge":0,
            "cgstRate":9,"sgstRate":9,"igstRate":0,"roundOff":0,"securityDeposit":0,"terms":"Net 30","notes":"Phase 6 test",
            "items":[{"itemId":%d,"quantity":100,"rate":20,"rentalType":"PER_PIECE_PER_DAY"},{"itemId":%d,"quantity":20,"rate":1,"rentalType":"PER_PIECE_PER_DAY"}]}
            """.formatted(quotationTemplateId, party.getId(), site.getId(), plate.getId(), prop.getId());
    }
}
