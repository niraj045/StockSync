package com.stocksync.challan.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.io.File;
import java.io.FileOutputStream;

public class TestStamper {
    public static void main(String[] args) throws Exception {
        SteelFabChallanTemplateStamper stamper = new SteelFabChallanTemplateStamper();
        SteelFabChallanTemplateStamper.ChallanFields fields = new SteelFabChallanTemplateStamper.ChallanFields(
            "CH-1234",
            "REF-001",
            LocalDate.now(),
            "John Doe Client",
            "123 Client Street, City",
            "GSTIN123456789",
            "Site 45, Area",
            "Mr. Contact",
            "MH-12-AB-3456",
            "Driver Dan",
            "9876543210",
            List.of(
                new SteelFabChallanTemplateStamper.ChallanLine("Steel Rods", new BigDecimal("100"), "Good condition"),
                new SteelFabChallanTemplateStamper.ChallanLine("Steel Plates", new BigDecimal("50"), "")
            ),
            List.of("Term 1", "Term 2")
        );
        byte[] pdf = stamper.stamp("pdf-templates/steelfab_challan_template.docx", fields);
        try (FileOutputStream fos = new FileOutputStream("/home/ainosoft/.gemini/antigravity/brain/6d2e0ca8-b88c-4321-a38b-4ee13001cdfd/artifacts/test_challan.pdf")) {
            fos.write(pdf);
        }
        System.out.println("Generated Delivery Challan PDF successfully");
    }
}
