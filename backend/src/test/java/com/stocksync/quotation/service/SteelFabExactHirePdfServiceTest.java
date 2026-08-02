package com.stocksync.quotation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksync.quotation.dto.QuotationItemResponse;
import com.stocksync.quotation.dto.QuotationResponse;
import com.stocksync.quotation.dto.SteelFabExactHireRequest;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

class SteelFabExactHirePdfServiceTest {
    @Test void producesFivePageExactDocumentAndSample() throws Exception {
        QuotationResponse quotation = quotation();
        var generated = new SteelFabExactHirePdfService(new ObjectMapper()).generate(quotation);
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(generated.content()))) {
            assertThat(document.getNumberOfPages()).isEqualTo(5);
        }
        Files.createDirectories(Path.of("target"));
        Files.write(Path.of("target", "steelfab-exact-hire-sample.pdf"), generated.content());
    }

    @Test void acceptsFewerAndDifferentMaterials() throws Exception {
        QuotationResponse q = quotation();
        List<QuotationItemResponse> materials = List.of(
                item("Adjustable Prop", "25", "20", "50", "3", "800"),
                item("Wooden Plank", "12", "10", "35", "3", "500"));
        when(q.items()).thenReturn(materials);
        var generated = new SteelFabExactHirePdfService(new ObjectMapper()).generate(q);
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(generated.content()))) {
            assertThat(document.getNumberOfPages()).isEqualTo(5);
        }
    }

    @Test void addsContinuationPageForMoreThanSevenMaterials() throws Exception {
        QuotationResponse q = quotation();
        List<QuotationItemResponse> materials = new ArrayList<>(q.items());
        materials.add(item("Adjustable Prop", "25", "20", "50", "3", "800"));
        when(q.items()).thenReturn(materials);
        var generated = new SteelFabExactHirePdfService(new ObjectMapper()).generate(q);
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(generated.content()))) {
            assertThat(document.getNumberOfPages()).isEqualTo(6);
        }
        Files.createDirectories(Path.of("target"));
        Files.write(Path.of("target", "steelfab-dynamic-continuation-sample.pdf"), generated.content());
    }

    @Test void fitsLongRealisticSiteNamesIntoTheExactTemplate() throws Exception {
        QuotationResponse q = quotation();
        when(q.siteName()).thenReturn("Temporary Staging Site With Extended Project Name");
        var generated = new SteelFabExactHirePdfService(new ObjectMapper()).generate(q);
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(generated.content()))) {
            assertThat(document.getNumberOfPages()).isEqualTo(5);
        }
    }

    private QuotationResponse quotation() {
        QuotationResponse q = mock(QuotationResponse.class);
        when(q.quotationTemplateCode()).thenReturn(SteelFabExactHirePdfService.TEMPLATE_CODE);
        when(q.quotationNumber()).thenReturn("SFE/22/2026-2027");when(q.quotationDate()).thenReturn(LocalDate.of(2026,7,22));
        when(q.partyName()).thenReturn("Rocks & Logs (India) Pvt. Ltd.");when(q.siteName()).thenReturn("Bandra Site");
        when(q.exactHire()).thenReturn(new SteelFabExactHireRequest("Mumbai.","Quotation for Supply of H frame Scaffolding Materials on Hire for Bandra Site.",7,
                "6 Months (180 days)",90,new BigDecimal("195"),new BigDecimal("50"),new BigDecimal("18"),new BigDecimal("252431"),3,
                "Hussain Golwala","Sales Executive","8451044007",null,null,null,null));
        List<QuotationItemResponse> items = List.of(
                item("H Frame","2772","1500","52","6","1600"),item("Bracing","5488","2400","26","6","1000"),
                item("20 Ft MS Pipe","175","175","71","6","300"),item("Plate Pipe","200","200","76","6","0"),
                item("Base Jack","200","200","25","6","350"),item("Platform","400","400","76","6","1500"),
                item("Coupler","1400","1400","7.5","6","100"));
        when(q.items()).thenReturn(items);when(q.subtotal()).thenReturn(new BigDecimal("1283550"));
        when(q.cgstRate()).thenReturn(BigDecimal.ZERO);when(q.sgstRate()).thenReturn(BigDecimal.ZERO);when(q.igstRate()).thenReturn(new BigDecimal("18"));
        when(q.totalTax()).thenReturn(new BigDecimal("231039"));when(q.grandTotal()).thenReturn(new BigDecimal("1514589"));
        when(q.securityDeposit()).thenReturn(new BigDecimal("700000"));return q;
    }

    private QuotationItemResponse item(String name,String required,String offered,String rate,String months,String replacement) {
        QuotationItemResponse item = mock(QuotationItemResponse.class);
        when(item.itemCodeSnapshot()).thenReturn(name);when(item.itemNameSnapshot()).thenReturn(name);when(item.unitSnapshot()).thenReturn("Nos.");
        when(item.requiredQuantity()).thenReturn(new BigDecimal(required));when(item.quantity()).thenReturn(new BigDecimal(offered));
        when(item.rate()).thenReturn(new BigDecimal(rate));when(item.hireMonths()).thenReturn(new BigDecimal(months));
        when(item.replacementRate()).thenReturn(new BigDecimal(replacement));
        when(item.amount()).thenReturn(new BigDecimal(offered).multiply(new BigDecimal(rate)).multiply(new BigDecimal(months)));
        return item;
    }
}
