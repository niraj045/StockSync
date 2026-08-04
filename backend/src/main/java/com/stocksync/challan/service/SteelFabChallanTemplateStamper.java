package com.stocksync.challan.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.core.io.ClassPathResource;

final class SteelFabChallanTemplateStamper {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    byte[] stamp(String templatePath, ChallanFields fields) {
        return generateDocxAndConvertToPdf(fields, "Delivery Challan");
    }

    byte[] stampReturn(String templatePath, ChallanFields fields) {
        return generateDocxAndConvertToPdf(fields, "Return Challan");
    }

    private byte[] generateDocxAndConvertToPdf(ChallanFields fields, String title) {
        String docxTemplatePath = "pdf-templates/SteelFab_Challan_Template.docx";
        try (InputStream is = new ClassPathResource(docxTemplatePath).getInputStream();
             XWPFDocument doc = new XWPFDocument(is)) {

            // 1. Title and Challan No. in Table 1
            XWPFTable t1 = doc.getTables().get(1);
            setCellText(t1.getRow(0).getCell(1), title, true, 14, ParagraphAlignment.CENTER);

            String challanNo = "Challan No.: " + (fields.number() != null ? fields.number() : "");
            setCellText(t1.getRow(0).getCell(2), challanNo, true, 9, ParagraphAlignment.RIGHT);

            // 2. Client / Site info in Table 2
            XWPFTable t2 = doc.getTables().get(2);

            String refNo = "Ref No.: " + (fields.refNo() != null ? fields.refNo() : "");
            setCellText(t2.getRow(0).getCell(0), refNo, false, 9, ParagraphAlignment.LEFT);

            String dateStr = "Date: " + date(fields.date());
            setCellText(t2.getRow(0).getCell(1), dateStr, false, 9, ParagraphAlignment.LEFT);

            String clientBlock = "Client's Name & Address:-\n" +
                    (fields.clientName() != null ? fields.clientName() : "") + "\n" +
                    (fields.clientAddress() != null ? fields.clientAddress() : "");
            setCellText(t2.getRow(1).getCell(0), clientBlock, false, 9, ParagraphAlignment.LEFT);

            String siteBlock = "Site Address:-\n" + (fields.siteAddress() != null ? fields.siteAddress() : "");
            setCellText(t2.getRow(1).getCell(1), siteBlock, false, 9, ParagraphAlignment.LEFT);

            String gstNo = "GST No.: " + (fields.clientGstin() != null ? fields.clientGstin() : "");
            setCellText(t2.getRow(2).getCell(0), gstNo, false, 9, ParagraphAlignment.LEFT);

            String contact = "Client's Contact Person: " + (fields.contactPerson() != null ? fields.contactPerson() : "");
            setCellText(t2.getRow(2).getCell(1), contact, false, 9, ParagraphAlignment.LEFT);

            // 3. Fill Table Items (Rows 4 to 17)
            int count = Math.min(14, fields.items().size());
            for (int idx = 0; idx < 14; idx++) {
                XWPFTableRow row = t2.getRow(4 + idx);
                if (idx < count) {
                    ChallanLine item = fields.items().get(idx);
                    setCellText(row.getCell(1), item.itemName(), false, 9, ParagraphAlignment.LEFT);
                    setCellText(row.getCell(2), quantity(item.quantity()), false, 9, ParagraphAlignment.CENTER);
                    setCellText(row.getCell(3), item.remarks(), false, 9, ParagraphAlignment.LEFT);
                } else {
                    setCellText(row.getCell(1), "", false, 9, ParagraphAlignment.LEFT);
                    setCellText(row.getCell(2), "", false, 9, ParagraphAlignment.CENTER);
                    setCellText(row.getCell(3), "", false, 9, ParagraphAlignment.LEFT);
                }
            }

            // 4. Vehicle / Driver Info (Row 14 to 17, Col 4)
            String vehicleNo = "Vehicle No.:- " + (fields.vehicleNumber() != null ? fields.vehicleNumber() : "");
            setCellText(t2.getRow(14).getCell(4), vehicleNo, false, 9, ParagraphAlignment.LEFT);

            String driverName = "Driver Name:- " + (fields.driverName() != null ? fields.driverName() : "");
            setCellText(t2.getRow(15).getCell(4), driverName, false, 9, ParagraphAlignment.LEFT);

            String driverNumber = "Driver Number:- " + (fields.driverPhone() != null ? fields.driverPhone() : "");
            setCellText(t2.getRow(16).getCell(4), driverNumber, false, 9, ParagraphAlignment.LEFT);

            setCellText(t2.getRow(17).getCell(4), "Driver Sign:-", false, 9, ParagraphAlignment.LEFT);

            // 5. Receiver Info (Row 18, Col 2)
            String receiverBlock = "Counted, Confirmed and Received on Behalf of\nabove Client by:-\nName: __________________\nMob No.: ________________\nSignature: ________________";
            setCellText(t2.getRow(18).getCell(2), receiverBlock, false, 8.5f, ParagraphAlignment.LEFT);

            // 6. Save filled DOCX to a temp file and convert to PDF
            File tempDocx = File.createTempFile("challan_", ".docx");
            try (FileOutputStream fos = new FileOutputStream(tempDocx)) {
                doc.write(fos);
            }

            File tempPdf = convertDocxToPdf(tempDocx);
            byte[] pdfBytes = java.nio.file.Files.readAllBytes(tempPdf.toPath());

            // Clean up temp files
            tempDocx.delete();
            tempPdf.delete();

            return pdfBytes;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate PDF from DOCX template", e);
        }
    }

    private File convertDocxToPdf(File tempDocx) throws Exception {
        File tempDir = tempDocx.getParentFile();
        ProcessBuilder pb = new ProcessBuilder(
            "libreoffice", "--headless", "--convert-to", "pdf",
            "--outdir", tempDir.getAbsolutePath(),
            tempDocx.getAbsolutePath()
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();

        // Consume output to prevent process hang
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Consume
            }
        }

        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new IOException("LibreOffice conversion failed with exit code " + exitCode);
        }

        String pdfName = tempDocx.getName().substring(0, tempDocx.getName().lastIndexOf('.')) + ".pdf";
        return new File(tempDir, pdfName);
    }

    private static void setCellText(XWPFTableCell cell, String text, boolean bold, float fontSizePt, ParagraphAlignment alignment) {
        while (cell.getParagraphs().size() > 1) {
            cell.removeParagraph(1);
        }
        XWPFParagraph p = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);
        p.setAlignment(alignment != null ? alignment : ParagraphAlignment.LEFT);

        while (!p.getRuns().isEmpty()) {
            p.removeRun(0);
        }

        if (text == null) text = "";
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                p.createRun().addBreak();
            }
            XWPFRun run = p.createRun();
            run.setText(lines[i]);
            run.setFontFamily("Arial");
            run.setFontSize((int) fontSizePt);
            run.setBold(bold);
        }
    }

    private static String date(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }

    private static String quantity(BigDecimal value) {
        if (value == null) return "";
        return value.stripTrailingZeros().toPlainString();
    }

    record ChallanFields(
            String number,
            String refNo,
            LocalDate date,
            String clientName,
            String clientAddress,
            String clientGstin,
            String siteAddress,
            String contactPerson,
            String vehicleNumber,
            String driverName,
            String driverPhone,
            List<ChallanLine> items,
            List<String> termsLines) {}

    record ChallanLine(String itemName, BigDecimal quantity, String remarks) {}
}
