package com.stocksync.challan.service;

import com.stocksync.challan.dto.IssuedChallanResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class IssuedChallanPdfService {
    private static final String TEMPLATE = "pdf-templates/steelfab_delivery_challan_exact_editable.docx";
    private final SteelFabChallanTemplateStamper stamper = new SteelFabChallanTemplateStamper();

    public IssuedChallanPdfService() {}

    public PdfDocument generate(IssuedChallanResponse challan) {
        List<SteelFabChallanTemplateStamper.ChallanLine> lines = challan.items().stream()
                .map(i -> new SteelFabChallanTemplateStamper.ChallanLine(i.itemName(), i.quantity(), i.unit()))
                .toList();
        var fields = new SteelFabChallanTemplateStamper.ChallanFields(
                challan.challanNumber(),
                challan.siteOrderNumber(),
                challan.dispatchDate(),
                challan.partyName(),
                challan.partyAddress(),
                challan.partyGstin(),
                siteBlock(challan),
                challan.siteContact(),
                challan.vehicleNumber(),
                challan.driverName(),
                "",
                lines,
                challan.termsAndConditions());
        String filename = "challan-" + challan.challanNumber().replaceAll("[^A-Za-z0-9.-]", "-") + ".pdf";
        return new PdfDocument(filename, stamper.stamp(TEMPLATE, fields));
    }

    private String siteBlock(IssuedChallanResponse challan) {
        String address = challan.siteAddress() == null ? "" : challan.siteAddress();
        if (challan.siteName() == null || challan.siteName().isBlank()) return address;
        return challan.siteName() + " " + address;
    }

    public record PdfDocument(String filename, byte[] content) {}
}
