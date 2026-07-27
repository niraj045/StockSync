package com.stocksync.payment.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.stocksync.payment.dto.PaymentDtos.PaymentResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class PaymentReceiptPdfService {
    private final SpringTemplateEngine templates;
    public PaymentReceiptPdfService(SpringTemplateEngine templates) { this.templates = templates; }
    public byte[] generate(PaymentResponse payment) {
        Context context = new Context();
        context.setVariables(Map.of("payment", payment));
        String html = templates.process("payment-receipt-pdf", context);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            new PdfRendererBuilder().withHtmlContent(html, null).toStream(output).run();
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate payment receipt PDF", e);
        }
    }
}
