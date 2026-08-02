package com.stocksync.common.pdf;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class PdfBranding {
    public static final String COMPANY_NAME = "SteelFab Scaffoldings & Engineering Pvt. Ltd.";

    private final String letterheadDataUri;

    public PdfBranding() {
        ClassPathResource letterhead = new ClassPathResource("pdf-assets/steelfab-letterhead.jpg");
        try (InputStream input = letterhead.getInputStream()) {
            letterheadDataUri = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(input.readAllBytes());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load the SteelFab PDF letterhead", exception);
        }
    }

    public String companyName() {
        return COMPANY_NAME;
    }

    public String letterheadDataUri() {
        return letterheadDataUri;
    }
}
