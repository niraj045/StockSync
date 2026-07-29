package com.stocksync.agreement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksync.common.exception.BusinessRuleException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Service
public class AgreementPdfTemplateAnalyzer {
    private static final Pattern REFERENCE = Pattern.compile("(?im)Ref\\.?\\s*No\\.?\\s*[:\\-]\\s*([^\\r\\n]+)");
    private static final Pattern DATE = Pattern.compile("(?im)^\\s*Date\\s*[:\\-]\\s*([^\\r\\n]+)");
    private static final Pattern GSTIN = Pattern.compile("\\b[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][0-9A-Z]Z[0-9A-Z]\\b");
    private static final Pattern SECURITY = Pattern.compile("(?is)security\\s+deposit.{0,80}?(?:Rs\\.?|INR|₹)\\s*([0-9,]+(?:\\.\\d{1,2})?)");
    private static final Pattern GRAND_TOTAL = Pattern.compile("(?im)Grand\\s+Total\\s+(?:Rs\\.?\\s*)?([0-9,]+(?:\\.\\d{1,2})?)");
    private static final Pattern PARTY_AFTER_TO = Pattern.compile("(?im)^\\s*To,?\\s*\\R\\s*([^\\r\\n]+)");
    private static final Pattern SITE = Pattern.compile("(?im)(?:for|at)\\s+([^\\r\\n.]{2,80}?\\s+Site)\\b");
    private static final Pattern MONTHS = Pattern.compile("(?i)(?:minimum\\s+hire\\s+period|minimum\\s+period|commitment).{0,80}?([0-9]+|one|two|three|four|five|six|seven|eight|nine|twelve)\\s+months?");
    private static final Pattern DAYS = Pattern.compile("(?i)minimum\\s+hire\\s+period.{0,60}?([0-9]+)\\s+days?");

    private final ObjectMapper objectMapper;

    public AgreementPdfTemplateAnalyzer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Analysis analyze(Path path) {
        try (PDDocument document = PDDocument.load(path.toFile())) {
            if (document.isEncrypted()) {
                throw new BusinessRuleException("ENCRYPTED_PDF_NOT_SUPPORTED", "Password-protected PDFs cannot be analysed");
            }
            String text = new PDFTextStripper().getText(document);
            if (text == null || text.strip().length() < 80) {
                return new Analysis(document.getNumberOfPages(), sha256(path), text == null ? "" : text,
                        Map.of(), List.of("No usable text layer was found. Run local OCR before mapping this scanned PDF."));
            }
            Map<String, String> fields = new LinkedHashMap<>();
            put(fields, "referenceNumber", match(REFERENCE, text));
            put(fields, "documentDate", match(DATE, text));
            put(fields, "partyName", match(PARTY_AFTER_TO, text));
            put(fields, "siteName", match(SITE, text));
            put(fields, "gstin", match(GSTIN, text));
            put(fields, "securityDeposit", match(SECURITY, text));
            put(fields, "grandTotal", match(GRAND_TOTAL, text));
            put(fields, "minimumHireMonths", match(MONTHS, text));
            put(fields, "minimumHireDays", match(DAYS, text));
            if (looksLikeItemTable(text)) {
                fields.put("itemsTable", "Detected columns for item, quantity, unit, rate and total");
            }
            List<String> warnings = new ArrayList<>();
            if (!fields.containsKey("partyName")) warnings.add("Party name was not detected.");
            if (!fields.containsKey("itemsTable")) warnings.add("A reusable items table was not detected.");
            if (fields.containsKey("minimumHireMonths") && fields.containsKey("minimumHireDays")) {
                warnings.add("Multiple minimum-hire rules were detected; confirm the contractual rule manually.");
            }
            warnings.add("Review every detected value before promoting this draft to a generating template.");
            return new Analysis(document.getNumberOfPages(), sha256(path), text.strip(), fields, warnings);
        } catch (IOException e) {
            throw new BusinessRuleException("PDF_ANALYSIS_FAILED", "Unable to read the uploaded PDF");
        }
    }

    public String json(Analysis analysis) {
        try {
            return objectMapper.writeValueAsString(Map.of("fields", analysis.fields(), "warnings", analysis.warnings()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to store PDF analysis", e);
        }
    }

    private static String match(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? clean(matcher.group(matcher.groupCount() == 0 ? 0 : 1)) : null;
    }

    private static void put(Map<String, String> fields, String key, String value) {
        if (value != null && !value.isBlank()) fields.put(key, value);
    }

    private static String clean(String value) {
        return value == null ? null : value.replace('\u00a0', ' ')
                .replaceAll("\\p{Cc}", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean looksLikeItemTable(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("items") && lower.contains("qty") && lower.contains("rate") &&
                (lower.contains("total amount") || lower.contains("amount rs"));
    }

    private static String sha256(Path path) throws IOException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public record Analysis(int pageCount, String checksum, String text, Map<String, String> fields, List<String> warnings) {}
}
