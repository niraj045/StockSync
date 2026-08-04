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
        return generateDocxAndConvertToPdf(templatePath, fields, "Delivery Challan");
    }

    byte[] stampReturn(String templatePath, ChallanFields fields) {
        return generateDocxAndConvertToPdf(templatePath, fields, "Return Challan");
    }

    private byte[] generateDocxAndConvertToPdf(String docxTemplatePath, ChallanFields fields, String title) {
        try (InputStream is = new ClassPathResource(docxTemplatePath).getInputStream();
             XWPFDocument doc = new XWPFDocument(is)) {

            // 1. Title and Challan No. in Table 1
            XWPFTable t1 = doc.getTables().get(1);
            setCellText(t1.getRow(0).getCell(1), title, true, 14, ParagraphAlignment.CENTER);

            String challanNo = (fields.number() != null ? fields.number() : "");
            setCellLabeledText(t1.getRow(0).getCell(2), "Challan No.: ", challanNo, 9, ParagraphAlignment.RIGHT);

            // 2. Client / Site info in Table 2
            XWPFTable t2 = doc.getTables().get(2);

            String refNo = (fields.refNo() != null ? fields.refNo() : "");
            setCellLabeledText(t2.getRow(0).getCell(0), "Ref No.: ", refNo, 9, ParagraphAlignment.LEFT);

            String dateStr = date(fields.date());
            setCellLabeledText(t2.getRow(0).getCell(1), "Date: ", dateStr, 9, ParagraphAlignment.LEFT);

            String clientBlock = (fields.clientName() != null ? fields.clientName() : "") + "\n" +
                    (fields.clientAddress() != null ? fields.clientAddress() : "");
            setCellLabeledText(t2.getRow(1).getCell(0), "Client's Name & Address:-\n", clientBlock, 9, ParagraphAlignment.LEFT);

            String siteBlock = (fields.siteAddress() != null ? fields.siteAddress() : "");
            setCellLabeledText(t2.getRow(1).getCell(1), "Site Address:-\n", siteBlock, 9, ParagraphAlignment.LEFT);

            String gstNo = (fields.clientGstin() != null ? fields.clientGstin() : "");
            setCellLabeledText(t2.getRow(2).getCell(0), "GST No.: ", gstNo, 9, ParagraphAlignment.LEFT);

            String contact = (fields.contactPerson() != null ? fields.contactPerson() : "");
            setCellLabeledText(t2.getRow(2).getCell(1), "Client's Contact Person: ", contact, 9, ParagraphAlignment.LEFT);

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
            String vehicleNo = (fields.vehicleNumber() != null ? fields.vehicleNumber() : "");
            setCellLabeledText(t2.getRow(14).getCell(4), "Vehicle No.:- ", vehicleNo, 9, ParagraphAlignment.LEFT);

            String driverName = (fields.driverName() != null ? fields.driverName() : "");
            setCellLabeledText(t2.getRow(15).getCell(4), "Driver Name:- ", driverName, 9, ParagraphAlignment.LEFT);

            String driverNumber = (fields.driverPhone() != null ? fields.driverPhone() : "");
            setCellLabeledText(t2.getRow(16).getCell(4), "Driver Number:- ", driverNumber, 9, ParagraphAlignment.LEFT);

            setCellLabeledText(t2.getRow(17).getCell(4), "Driver Sign:- ", "", 9, ParagraphAlignment.LEFT);

            // 5. Receiver Info (Row 18, Col 2)
            String receiverBlock = "\nName: __________________\nMob No.: ________________\nSignature: ________________";
            setCellLabeledText(t2.getRow(18).getCell(2), "Counted, Confirmed and Received on Behalf of\nabove Client by:-", receiverBlock, 8.5f, ParagraphAlignment.LEFT);

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
        clearCell(cell, alignment);
        XWPFParagraph p = cell.getParagraphs().get(0);
        if (text == null) text = "";
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) p.createRun().addBreak();
            addRun(p, lines[i], bold, fontSizePt);
        }
    }

    private static void setCellLabeledText(XWPFTableCell cell, String label, String value, float fontSizePt, ParagraphAlignment alignment) {
        clearCell(cell, alignment);
        XWPFParagraph p = cell.getParagraphs().get(0);
        if (label != null && !label.isEmpty()) {
            addRun(p, label, true, fontSizePt);
        }
        if (value != null && !value.isEmpty()) {
            addRun(p, value, false, fontSizePt);
        }
    }

    private static void clearCell(XWPFTableCell cell, ParagraphAlignment alignment) {
        while (cell.getParagraphs().size() > 1) {
            cell.removeParagraph(1);
        }
        XWPFParagraph p = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().get(0);
        p.setAlignment(alignment != null ? alignment : ParagraphAlignment.LEFT);
        while (!p.getRuns().isEmpty()) {
            p.removeRun(0);
        }
    }

    private static void addRun(XWPFParagraph p, String text, boolean bold, float fontSizePt) {
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontFamily("Arial");
        run.setFontSize((int) fontSizePt);
        run.setBold(bold);
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
