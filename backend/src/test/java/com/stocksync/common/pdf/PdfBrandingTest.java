package com.stocksync.common.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class PdfBrandingTest {
    private static final List<String> OPERATIONAL_TEMPLATES = List.of(
            "templates/order-pdf.html",
            "templates/issued-challan-pdf.html",
            "templates/receiving-challan-pdf.html",
            "templates/site-transfer-pdf.html");

    @Test
    void providesApprovedSteelFabJpegLetterhead() {
        PdfBranding branding = new PdfBranding();

        assertThat(branding.companyName())
                .isEqualTo("SteelFab Scaffoldings & Engineering Pvt. Ltd.");
        assertThat(branding.letterheadDataUri()).startsWith("data:image/jpeg;base64,");

        byte[] image = Base64.getDecoder().decode(
                branding.letterheadDataUri().substring("data:image/jpeg;base64,".length()));
        assertThat(image).startsWith((byte) 0xff, (byte) 0xd8);
        assertThat(image.length).isGreaterThan(10_000);
    }

    @Test
    void operationalTemplatesUseSharedSteelFabBranding() throws IOException {
        for (String path : OPERATIONAL_TEMPLATES) {
            String template = new ClassPathResource(path)
                    .getContentAsString(StandardCharsets.UTF_8);

            assertThat(template).contains("letterheadDataUri", "companyName");
            assertThat(template).doesNotContain("StockSync");
        }
    }
}
