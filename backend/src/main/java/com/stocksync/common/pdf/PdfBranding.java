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
    private final String stampDataUri;

    public PdfBranding() {
        letterheadDataUri = loadJpeg("pdf-assets/steelfab-letterhead.jpg", "letterhead");
        stampDataUri = loadJpeg("pdf-assets/steelfab-stamp-source.jpg", "stamp");
    }

    private String loadJpeg(String path, String description) {
        ClassPathResource image = new ClassPathResource(path);
        try (InputStream input = image.getInputStream()) {
            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(input.readAllBytes());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load the SteelFab PDF " + description, exception);
        }
    }

    public String companyName() {
        return COMPANY_NAME;
    }

    public String letterheadDataUri() {
        return letterheadDataUri;
    }

    public String stampDataUri() {
        return stampDataUri;
    }
}
