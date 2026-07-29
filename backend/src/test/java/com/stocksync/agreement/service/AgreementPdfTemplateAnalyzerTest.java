package com.stocksync.agreement.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AgreementPdfTemplateAnalyzerTest {

    @TempDir
    Path tempDir;

    @Test
    void extractsReusableAgreementFieldsWithoutAnExternalAiService() throws Exception {
        Path pdf = tempDir.resolve("client-agreement.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA, 10);
                content.newLineAtOffset(50, 750);
                for (String line : new String[]{
                        "Ref. No.: SFE/22/2026-2027",
                        "Date: 22nd July 2026",
                        "To,",
                        "Rocks & Logs (India) Pvt. Ltd.",
                        "Supply of materials for Bandra Site",
                        "GSTIN 27ABCDE1234F1Z5",
                        "Items Qty Unit Rate Total Amount",
                        "Security Deposit INR 7,00,000",
                        "Grand Total Rs. 15,14,589.00",
                        "Minimum hire period is 180 days"
                }) {
                    content.showText(line);
                    content.newLineAtOffset(0, -18);
                }
                content.endText();
            }
            document.save(pdf.toFile());
        }

        var analysis = new AgreementPdfTemplateAnalyzer(new ObjectMapper()).analyze(pdf);

        assertThat(analysis.pageCount()).isEqualTo(1);
        assertThat(analysis.checksum()).hasSize(64);
        assertThat(analysis.fields())
                .containsEntry("referenceNumber", "SFE/22/2026-2027")
                .containsEntry("partyName", "Rocks & Logs (India) Pvt. Ltd.")
                .containsEntry("siteName", "Bandra Site")
                .containsEntry("minimumHireDays", "180")
                .containsKey("itemsTable");
        assertThat(Files.size(pdf)).isPositive();
    }
}
