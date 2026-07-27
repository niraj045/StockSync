package com.stocksync.challan.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.stocksync.challan.dto.ReceivingChallanResponse;
import java.io.*;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class ReceivingChallanPdfService {
    private final SpringTemplateEngine templates;

    public ReceivingChallanPdfService(SpringTemplateEngine templates) {
        this.templates = templates;
    }

    public PdfDocument generate(ReceivingChallanResponse challan) {
        Context context = new Context();
        context.setVariables(Map.of("c", challan));
        String html = templates.process("receiving-challan-pdf", context);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            new PdfRendererBuilder().withHtmlContent(html, null).toStream(output).run();
            String filename = "receiving-challan-" + challan.receivingChallanNumber().replaceAll("[^A-Za-z0-9.-]", "-") + ".pdf";
            return new PdfDocument(filename, output.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate receiving challan PDF", e);
        }
    }

    public record PdfDocument(String filename, byte[] content) {}
}
