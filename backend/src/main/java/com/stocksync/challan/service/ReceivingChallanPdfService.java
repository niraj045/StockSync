package com.stocksync.challan.service;

import com.stocksync.challan.dto.ReceivingChallanResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReceivingChallanPdfService {
    private static final String TEMPLATE = "pdf-templates/steelfab_challan_template.docx";
    private final SteelFabChallanTemplateStamper stamper = new SteelFabChallanTemplateStamper();

    public ReceivingChallanPdfService() {}

    public PdfDocument generate(ReceivingChallanResponse challan) {
        List<SteelFabChallanTemplateStamper.ChallanLine> lines = challan.items().stream()
                .map(i -> new SteelFabChallanTemplateStamper.ChallanLine(i.itemName(), returned(i), i.notes()))
                .toList();
        var fields = new SteelFabChallanTemplateStamper.ChallanFields(
                challan.receivingChallanNumber(),
                challan.refNo() != null ? challan.refNo() : (challan.linkedIssuedChallanNumber() == null ? challan.agreementNumber() : challan.linkedIssuedChallanNumber()),
                challan.receiveDate(),
                challan.partyName(),
                challan.partyAddress(),
                challan.partyGstin(),
                siteBlock(challan),
                challan.siteContact(),
                challan.vehicleNumber(),
                challan.driverName(),
                challan.driverPhone(),
                lines,
                challan.termsAndConditions());
        String filename = "receiving-challan-" + challan.receivingChallanNumber().replaceAll("[^A-Za-z0-9.-]", "-") + ".pdf";
        return new PdfDocument(filename, stamper.stampReturn(TEMPLATE, fields));
    }

    private BigDecimal returned(com.stocksync.challan.dto.ReceivingChallanItemResponse item) {
        return add(item.goodReturnedQuantity(), item.damagedReturnedQuantity(), item.extraReturnedQuantity());
    }

    private BigDecimal add(BigDecimal... values) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : values) if (value != null) total = total.add(value);
        return total;
    }

    private String siteBlock(ReceivingChallanResponse challan) {
        String address = challan.siteAddress() == null ? "" : challan.siteAddress();
        if (challan.siteName() == null || challan.siteName().isBlank()) return address;
        return challan.siteName() + " " + address;
    }

    public record PdfDocument(String filename, byte[] content) {}
}
