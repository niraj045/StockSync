package com.stocksync.quotation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.quotation.dto.QuotationItemResponse;
import com.stocksync.quotation.dto.QuotationResponse;
import com.stocksync.quotation.dto.SteelFabExactHireRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class SteelFabExactHirePdfService {
    public static final String TEMPLATE_CODE = "STEELFAB_EXACT_HIRE_V1";
    private static final String PDF_PATH = "pdf-templates/steelfab-exact-hire-v1.pdf";
    private static final String CONFIG_PATH = "pdf-templates/steelfab-exact-hire-v1-fields.json";
    private static final int PRIMARY_TABLE_ROWS = 7;
    private static final int CONTINUATION_TABLE_ROWS = 18;
    private final ExactPdfConfig config;
    private final SteelFabExactHireFormatter format = new SteelFabExactHireFormatter();

    public SteelFabExactHirePdfService(ObjectMapper objectMapper) {
        try (InputStream input = new ClassPathResource(CONFIG_PATH).getInputStream()) {
            this.config = objectMapper.readValue(input, ExactPdfConfig.class);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load SteelFab exact PDF coordinates", e);
        }
    }

    public int coordinatesVersion() { return config.coordinatesVersion(); }

    public PdfDocument generate(QuotationResponse quotation) {
        requireExactTemplate(quotation);
        SteelFabExactHireRequest exact = quotation.exactHire();
        if (exact == null) throw new BusinessRuleException("EXACT_HIRE_FIELDS_REQUIRED", "Exact SteelFab PDF details are required");
        if (quotation.items() == null || quotation.items().isEmpty())
            throw new BusinessRuleException("QUOTATION_ITEMS_REQUIRED", "At least one SteelFab material is required");
        try (InputStream source = new ClassPathResource(PDF_PATH).getInputStream();
             PDDocument document = PDDocument.load(source);
             InputStream continuationSource = new ClassPathResource(PDF_PATH).getInputStream();
             PDDocument continuationTemplate = PDDocument.load(continuationSource);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (document.getNumberOfPages() != config.pageCount()) {
                throw new IllegalStateException("SteelFab source PDF must contain exactly five pages");
            }
            stamp(document, quotation, exact, quotation.items());
            appendContinuationSchedules(document, continuationTemplate, quotation.items());
            document.save(output);
            return new PdfDocument(filename(quotation), output.toByteArray(), config.templateVersion(), config.coordinatesVersion());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate SteelFab exact PDF", e);
        }
    }

    private void stamp(PDDocument document, QuotationResponse q, SteelFabExactHireRequest e,
            List<QuotationItemResponse> items) throws IOException {
        int validity = e.validityDays() == null ? 7 : e.validityDays();
        String minimumPeriod=value(e.minimumHirePeriod(), "6 Months (180 days)");
        drawPageOne(document, q, e, items, validity, minimumPeriod);
        BigDecimal gst = e.gstPercentage() == null ? q.cgstRate().add(q.sgstRate()).add(q.igstRate()) : e.gstPercentage();
        int minimumDays = e.minimumHireDays() == null ? 90 : e.minimumHireDays();
        int dueDays = e.paymentDueDays() == null ? 3 : e.paymentDueDays();
        drawPageTwo(document, q, e, items, gst);
        drawPageThree(document, q);
        drawPageFour(document, q, e, items, minimumDays, dueDays);
        drawPageFive(document, q, e);
    }

    private void drawPageOne(PDDocument document, QuotationResponse q, SteelFabExactHireRequest e,
            List<QuotationItemResponse> items, int validity, String minimumPeriod) throws IOException {
        fillWhite(document, 0, 42, 96, 512, 690);
        float x = 56;
        drawFittedText(document, 0, x, 110, 54, 16, 10.5f, "LEFT", true, "Ref. No.:");
        drawFittedText(document, 0, 112, 110, 260, 16, 10.5f, "LEFT", false, q.quotationNumber());
        drawFittedText(document, 0, x, 127, 54, 16, 10.5f, "LEFT", true, "Date:");
        drawFittedText(document, 0, 112, 127, 220, 16, 10.5f, "LEFT", false, format.dated(q.quotationDate()));

        drawFittedText(document, 0, x, 164, 50, 15, 10.5f, "LEFT", false, "To,");
        drawFittedText(document, 0, x, 181, 483, 17, 11f, "LEFT", true, q.partyName());
        float addressEnd = drawWrappedBlock(document, 0, x, 200, 483, 10.5f, false, e.partyAddress());

        float subjectTop = Math.max(232, addressEnd + 10);
        drawFittedText(document, 0, x, subjectTop, 35, 16, 10.5f, "LEFT", true, "Sub:");
        float subjectEnd = drawWrappedBlock(document, 0, 91, subjectTop, 448, 10.5f, false,
                value(e.subject(), "Quotation for supply of scaffolding materials on hire for " + q.siteName() + "."));

        float cursor = subjectEnd + 18;
        drawFittedText(document, 0, x, cursor, 100, 16, 10.5f, "LEFT", false, "Dear Sir,");
        cursor += 29;
        String introduction = "With reference to our recent discussion and your enquiry, we are pleased to submit this quotation for hiring scaffolding materials for your project. "
                + "This quotation is valid for " + validity + " (" + format.integerWords(validity) + ") days from the date of issue. "
                + "If the order is confirmed after the validity period, hire charges will be subject to revision based on the rates prevailing on the confirmation date.";
        cursor = drawWrappedBlock(document, 0, x, cursor, 483, 10.5f, false, introduction) + 20;

        drawFittedText(document, 0, x, cursor, 300, 17, 11f, "LEFT", true, "PART A: HIRE CHARGES & COSTS");
        cursor += 28;
        cursor = drawWrappedBlock(document, 0, x, cursor, 483, 10.5f, false,
                "The hire charges for the scaffolding materials are detailed below. These rates are based on a minimum hire period of " + minimumPeriod + ".") + 20;
        drawFittedText(document, 0, x, cursor, 300, 17, 11f, "LEFT", true, "Important Notes on Costs");
        cursor += 27;
        cursor = drawWrappedBlock(document, 0, x + 12, cursor, 471, 10.5f, false,
                "- The charges stated below are for material hire only.") + 7;
        cursor = drawWrappedBlock(document, 0, x + 12, cursor, 471, 10.5f, false,
                "- The rates are based on your commitment to the stated minimum hire period.") + 18;
        cursor = drawWrappedBlock(document, 0, x, cursor, 483, 10.5f, false,
                "Based on the " + q.siteName() + " measurement of " + format.quantity(e.siteLengthRmt())
                        + " RMT length and " + format.quantity(e.siteHeightMtr())
                        + " MTR height, the approximate required quantities are:") + 13;
        int visible = Math.min(items.size(), 7);
        for (int index = 0; index < visible; index++) {
            QuotationItemResponse item = items.get(index);
            cursor = drawWrappedBlock(document, 0, x + 8, cursor, 475, 9.5f, false,
                    item.itemNameSnapshot() + " - " + format.quantity(item.requiredQuantity()) + " " + unit(item.unitSnapshot())) + 3;
        }
        if (items.size() > visible) {
            drawWrappedBlock(document, 0, x + 8, cursor, 475, 9f, true,
                    "+ " + (items.size() - visible) + " additional materials are listed in the attached schedule.");
        }
    }

    private float drawWrappedBlock(PDDocument document, int pageIndex, float x, float top, float width,
            float fontSize, boolean bold, String value) throws IOException {
        if (value == null || value.isBlank()) return top;
        PDFont font = bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;
        List<String> lines = wrap(font, fontSize, width, value);
        float lineHeight = fontSize * 1.32f;
        PDPage page = document.getPage(pageIndex);
        try (PDPageContentStream stream = new PDPageContentStream(document, page,
                PDPageContentStream.AppendMode.APPEND, true, true)) {
            stream.setNonStrokingColor(0, 0, 0);
            stream.setFont(font, fontSize);
            for (int index = 0; index < lines.size(); index++) {
                stream.beginText();
                stream.newLineAtOffset(x, page.getMediaBox().getHeight() - top - fontSize - index * lineHeight);
                stream.showText(lines.get(index));
                stream.endText();
            }
        }
        return top + lines.size() * lineHeight;
    }

    private void drawPageTwo(PDDocument document, QuotationResponse q, SteelFabExactHireRequest e,
            List<QuotationItemResponse> items, BigDecimal gst) throws IOException {
        clearBody(document, 1);
        float x=56, cursor=112;
        cursor=drawWrappedBlock(document,1,x,cursor,483,10.5f,true,
                "Currently available material and monthly hire charges")+16;
        cursor=drawWrappedBlock(document,1,x,cursor,483,10.5f,false,
                "The following quantities can presently be supplied against this quotation.")+14;
        cursor=drawMaterialSchedule(document,1,cursor,items.subList(0,Math.min(PRIMARY_TABLE_ROWS,items.size())))+14;
        drawFittedText(document,1,315,cursor,128,16,10f,"RIGHT",true,"Subtotal");
        drawFittedText(document,1,450,cursor,89,16,10f,"RIGHT",true,format.money(q.subtotal()));cursor+=19;
        drawFittedText(document,1,315,cursor,128,16,10f,"RIGHT",false,"GST "+format.quantity(gst)+"%");
        drawFittedText(document,1,450,cursor,89,16,10f,"RIGHT",false,format.money(q.totalTax()));cursor+=19;
        drawLine(document,1,315,cursor,539);cursor+=5;
        drawFittedText(document,1,315,cursor,128,17,11f,"RIGHT",true,"Grand Total");
        drawFittedText(document,1,450,cursor,89,17,11f,"RIGHT",true,format.money(q.grandTotal()));cursor+=28;
        cursor=drawWrappedBlock(document,1,x,cursor,483,10f,true,"Amount in words: "+format.amountInWords(q.grandTotal()))+22;
        cursor=drawWrappedBlock(document,1,x,cursor,483,10.5f,false,
                "The above rates are exclusive of to-and-fro transport and Mathadi Union payments, which shall fall within your scope. We are liable only for loading and unloading of materials at our godown. During delivery, please depute your representative at our godown for proper counting and signing of the delivery challans. After your representative signs the challans, we shall not be responsible for claims of short or incorrect delivery.")+24;
        drawFittedText(document,1,x,cursor,420,17,10.5f,"LEFT",true,
                "For SteelFab Scaffoldings & Engineering Private Limited");cursor+=26;
        drawFittedText(document,1,x,cursor,260,17,10.5f,"LEFT",true,value(e.authorizedPerson(),"Hussain Golwala"));
    }

    private void drawPageThree(PDDocument document, QuotationResponse q) throws IOException {
        clearBody(document,2);float x=56,cursor=112;
        drawFittedText(document,2,x,cursor,483,18,12f,"LEFT",true,"PART B: TERMS AND CONDITIONS");cursor+=29;
        cursor=drawWrappedBlock(document,2,x,cursor,483,10.5f,false,
                "Please read these terms carefully. They form the basis of the material hire agreement.")+24;
        cursor=section(document,2,1,"Definitions",cursor);
        cursor=bullet(document,2,cursor,"Owner / We / Us refers to SteelFab Scaffoldings & Engineering Private Limited, the company providing the scaffolding materials on hire.");
        cursor=bullet(document,2,cursor,"Hirer / You / Your refers to "+q.partyName()+", to whom the scaffolding materials are being hired.");
        cursor=bullet(document,2,cursor,"Scaffolding Materials means all equipment listed in the material schedule above.");
        cursor=bullet(document,2,cursor,"The hire period starts on the day the first lot of materials is delivered to your site. It continues until you give us written confirmation that all materials are ready for collection.")+13;
        cursor=section(document,2,2,"Ownership and Responsibility",cursor);
        cursor=bullet(document,2,cursor,"The scaffolding materials always remain the sole property of SteelFab Scaffoldings & Engineering Private Limited. This agreement is for hire only and not for sale.");
        cursor=bullet(document,2,cursor,"You are responsible for the safety and security of all materials after delivery. Lost, stolen, or damaged material must be reimbursed at its full replacement value.")+13;
        cursor=section(document,2,3,"Hirer's Responsibilities",cursor);
        cursor=bullet(document,2,cursor,"You are responsible for inspecting the materials while loading for delivery. Damaged or non-working items must be reported immediately; otherwise, the materials will be deemed accepted.");
        cursor=bullet(document,2,cursor,"You must not sub-hire, sell, or part with possession of any scaffolding material to a third party without our explicit written consent.");
        bullet(document,2,cursor,"At the end of the hire period, materials must be returned in the condition received. Damage, different sizes, or shortages will be deducted from the deposit at the agreed clause 4 rates.");
    }

    private void drawPageFour(PDDocument document, QuotationResponse q, SteelFabExactHireRequest e,
            List<QuotationItemResponse> items,int minimumDays,int dueDays) throws IOException {
        clearBody(document,3);float cursor=112;
        cursor=section(document,3,4,"Hire Period, Returns, and Loss/Damage Charges",cursor);
        cursor=bullet(document,3,cursor,"The minimum hire period is "+minimumDays+" days. If materials are returned before "+minimumDays+" days, you will still be charged rent for the full "+minimumDays+" days.");
        cursor=bullet(document,3,cursor,"If you need the materials for a longer period, hire charges will continue month to month at the same rates until the materials are delivered back to our warehouse.");
        cursor=bullet(document,3,cursor,"The hire period for lost or unreturned material will cease only when we receive payment for its full replacement value.")+8;
        drawFittedText(document,3,74,cursor,465,16,10.5f,"LEFT",true,"Replacement cost for lost or damaged items");cursor+=21;
        for(QuotationItemResponse item:items.subList(0,Math.min(7,items.size()))){BigDecimal rate=item.replacementRate()==null?BigDecimal.ZERO:item.replacementRate();cursor=drawWrappedBlock(document,3,84,cursor,455,9.5f,false,item.itemNameSnapshot()+" - Rs. "+rate.stripTrailingZeros().toPlainString()+" per "+unit(item.unitSnapshot()).toLowerCase())+2;}
        cursor+=12;cursor=section(document,3,5,"Security and Advance Payment",cursor);
        cursor=drawWrappedBlock(document,3,74,cursor,465,10.5f,false,"A refundable security deposit of Rs. "+format.quantity(q.securityDeposit())+"/- ("+format.amountInWords(q.securityDeposit())+") is payable before dispatch. It will be refunded without interest after dues are deducted and material is returned in good condition.")+10;
        cursor=drawWrappedBlock(document,3,74,cursor,465,10.5f,false,"One month's advance rent of Rs. "+format.quantity(e.advanceRent())+"/- is payable toward hire charges.")+14;
        cursor=section(document,3,6,"Invoicing and Payment Terms",cursor);
        cursor=drawWrappedBlock(document,3,74,cursor,465,10.5f,false,"We will raise an invoice monthly. Payment is due within "+dueDays+" days from the invoice date. If payment is delayed beyond "+dueDays+" ("+format.integerWords(dueDays)+") days, the matter shall be discussed mutually.")+14;
        cursor=section(document,3,7,"Transportation",cursor);
        cursor=drawWrappedBlock(document,3,74,cursor,465,10.5f,false,"All costs of transporting materials to your site and back to our yard will be borne by you.")+14;
        cursor=section(document,3,8,"Delivery and Delays",cursor);
        cursor=drawWrappedBlock(document,3,74,cursor,465,10.5f,false,"We will make every effort to deliver materials as per your schedule. However, we shall not be responsible for delays beyond our reasonable control, including accidents, traffic, natural disasters, government restrictions, or unavailability of transport.")+14;
        cursor=section(document,3,9,"Cancellation",cursor);
        drawWrappedBlock(document,3,74,cursor,465,10.5f,false,"If you cancel this order after acceptance, you will be responsible for reimbursing expenses already incurred, including arranged transport and administrative costs, up to the point of cancellation.");
    }

    private void drawPageFive(PDDocument document, QuotationResponse q, SteelFabExactHireRequest e) throws IOException {
        clearBody(document,4);float x=56,cursor=112;
        cursor=section(document,4,10,"Complete Contract Agreement",cursor);
        cursor=drawWrappedBlock(document,4,x,cursor,483,10.5f,false,"This document, along with your work order and our acceptance, constitutes the entire contract agreement between us. Any changes to these terms must be agreed upon in writing by both parties.")+22;
        cursor=drawWrappedBlock(document,4,x,cursor,483,10.5f,false,"We hope this quotation meets your requirements. If the terms are acceptable, please sign below to confirm your order. We look forward to a positive response and a successful partnership.")+24;
        drawFittedText(document,4,x,cursor,100,16,10.5f,"LEFT",false,"Thank you,");cursor+=20;
        drawFittedText(document,4,x,cursor,200,16,10.5f,"LEFT",true,"Yours faithfully,");cursor+=18;
        drawFittedText(document,4,x,cursor,450,17,10.5f,"LEFT",true,"For SteelFab Scaffoldings & Engineering Private Limited");cursor+=34;
        drawFittedText(document,4,x,cursor,280,17,10.5f,"LEFT",true,value(e.authorizedPerson(),"Hussain Golwala"));cursor+=18;
        drawFittedText(document,4,x,cursor,280,17,10.5f,"LEFT",false,value(e.authorizedDesignation(),"Sales Executive"));cursor+=18;
        drawFittedText(document,4,x,cursor,280,17,10.5f,"LEFT",false,"Ph. No.: "+value(e.authorizedPhone(),"8451044007"));cursor+=48;
        drawFittedText(document,4,x,cursor,250,17,10.5f,"LEFT",false,"Accepted and confirmed by");cursor+=19;
        drawFittedText(document,4,x,cursor,483,17,11f,"LEFT",true,q.partyName());cursor+=52;
        drawFittedText(document,4,x,cursor,200,17,10.5f,"LEFT",true,"Signature and seal");cursor+=48;
        drawAcceptanceLine(document,4,cursor,"Name",e.acceptedBy());cursor+=34;
        drawAcceptanceLine(document,4,cursor,"Designation",e.acceptedDesignation());cursor+=34;
        drawAcceptanceLine(document,4,cursor,"Ph. No.",e.acceptedPhone());cursor+=34;
        drawAcceptanceLine(document,4,cursor,"Date",format.date(e.acceptedDate()));
    }

    private void clearBody(PDDocument document,int pageIndex)throws IOException{fillWhite(document,pageIndex,42,96,512,690);}
    private float section(PDDocument document,int page,int number,String title,float top)throws IOException{drawFittedText(document,page,56,top,483,18,11f,"LEFT",true,number+".  "+title);return top+26;}
    private float bullet(PDDocument document,int page,float top,String text)throws IOException{return drawWrappedBlock(document,page,74,top,465,10.5f,false,"- "+text)+9;}
    private void drawLine(PDDocument document,int pageIndex,float x1,float top,float x2)throws IOException{PDPage page=document.getPage(pageIndex);float y=page.getMediaBox().getHeight()-top;try(PDPageContentStream stream=new PDPageContentStream(document,page,PDPageContentStream.AppendMode.APPEND,true,true)){stream.setStrokingColor(40,40,40);stream.setLineWidth(.6f);stream.moveTo(x1,y);stream.lineTo(x2,y);stream.stroke();}}
    private void drawAcceptanceLine(PDDocument document,int page,float top,String label,String value)throws IOException{drawFittedText(document,page,56,top,75,17,10.5f,"LEFT",true,label+":");drawFittedText(document,page,135,top,300,17,10.5f,"LEFT",false,value==null||value.isBlank()?"____________________________":value);}

    private float drawMaterialSchedule(PDDocument document,int pageIndex,float top,List<QuotationItemResponse> items)throws IOException{
        float[] widths={36,137,54,48,70,50,88};float x=56,header=34,row=21;
        String[] labels={"#","Item","Qty","Unit","Rate / month","Months","Amount"};
        for(int i=0;i<labels.length;i++)drawFittedText(document,pageIndex,x+before(widths,i)+2,top+7,widths[i]-4,22,8.5f,"CENTER",true,labels[i]);
        for(int i=0;i<items.size();i++)drawMaterialRow(document,pageIndex,x,top+header+i*row,widths,row,i+1,items.get(i));
        drawGrid(document,pageIndex,x,top,widths,row,items.size(),header);return top+header+items.size()*row;
    }

    private void draw(PDDocument document, String key, String value) throws IOException {
        if (value == null || value.isBlank()) return;
        List<FieldArea> areas = config.fields().get(key);
        if (areas == null) throw new IllegalStateException("Missing PDF coordinate field: " + key);
        for (FieldArea area : areas) drawArea(document, area, value);
    }

    private void drawRequiredMaterials(PDDocument document, List<QuotationItemResponse> items) throws IOException {
        fillWhite(document, 0, 43, 546, 330, 111);
        int visible = Math.min(items.size(), 7);
        for (int index = 0; index < visible; index++) {
            QuotationItemResponse item = items.get(index);
            String text = item.itemNameSnapshot() + " - " + format.quantity(item.requiredQuantity()) + " " + unit(item.unitSnapshot());
            drawFittedText(document, 0, 45, 550 + index * 14.5f, 320, 12, 9, "LEFT", false, text);
        }
        if (items.size() > visible) {
            drawFittedText(document, 0, 45, 550 + 6 * 14.5f, 320, 12, 8, "LEFT", true,
                    "+ " + (items.size() - 6) + " additional materials listed in the attached schedule.");
        }
    }

    private void drawPrimaryMaterialTable(PDDocument document, List<QuotationItemResponse> items) throws IOException {
        float[] widths = {48, 84, 48, 48, 47, 48, 96};
        float x = 82, top = 222.5f, rowHeight = 15.5f;
        fillWhite(document, 1, x, top, sum(widths), rowHeight * PRIMARY_TABLE_ROWS);
        for (int row = 0; row < PRIMARY_TABLE_ROWS; row++) {
            if (row < items.size()) drawMaterialRow(document, 1, x, top + row * rowHeight, widths, rowHeight, row + 1, items.get(row));
        }
        drawGrid(document, 1, x, top, widths, rowHeight, PRIMARY_TABLE_ROWS, 0);
    }

    private void appendContinuationSchedules(PDDocument document, PDDocument template,
            List<QuotationItemResponse> items) throws IOException {
        if (items.size() <= PRIMARY_TABLE_ROWS) return;
        List<QuotationItemResponse> remaining = items.subList(PRIMARY_TABLE_ROWS, items.size());
        for (int offset = 0; offset < remaining.size(); offset += CONTINUATION_TABLE_ROWS) {
            int end = Math.min(offset + CONTINUATION_TABLE_ROWS, remaining.size());
            document.importPage(template.getPage(1));
            int pageIndex = document.getNumberOfPages() - 1;
            fillWhite(document, pageIndex, 45, 135, 505, 670);
            drawFittedText(document, pageIndex, 56, 152, 483, 22, 13, "CENTER", true, "MATERIAL SCHEDULE - CONTINUED");
            drawFittedText(document, pageIndex, 56, 177, 483, 16, 9, "LEFT", false,
                    "Additional materials forming part of the same SteelFab hire quotation.");
            drawContinuationTable(document, pageIndex, remaining.subList(offset, end), PRIMARY_TABLE_ROWS + offset + 1);
        }
    }

    private void drawContinuationTable(PDDocument document, int pageIndex, List<QuotationItemResponse> items, int firstSerial) throws IOException {
        float[] widths = {35, 145, 55, 50, 65, 55, 100};
        String[] headers = {"Sr.No.", "Items", "Qty", "Unit", "Rate / Month", "Months", "Total Amount Rs."};
        float x = 56, top = 205, headerHeight = 36, rowHeight = 22;
        for (int column = 0; column < headers.length; column++) {
            drawFittedText(document, pageIndex, x + before(widths, column) + 2, top + 7,
                    widths[column] - 4, 24, 8, "CENTER", true, headers[column]);
        }
        for (int row = 0; row < items.size(); row++) {
            drawMaterialRow(document, pageIndex, x, top + headerHeight + row * rowHeight, widths, rowHeight,
                    firstSerial + row, items.get(row));
        }
        drawGrid(document, pageIndex, x, top, widths, rowHeight, items.size(), headerHeight);
    }

    private void drawMaterialRow(PDDocument document, int pageIndex, float x, float top, float[] widths,
            float rowHeight, int serial, QuotationItemResponse item) throws IOException {
        String[] values = {String.valueOf(serial), item.itemNameSnapshot(), format.quantity(item.quantity()),
                unit(item.unitSnapshot()), format.money(item.rate()), format.quantity(item.hireMonths()), format.money(item.amount())};
        String[] alignments = {"CENTER", "LEFT", "CENTER", "CENTER", "RIGHT", "CENTER", "RIGHT"};
        for (int column = 0; column < values.length; column++) {
            drawFittedText(document, pageIndex, x + before(widths, column) + 2, top + 2,
                    widths[column] - 4, rowHeight - 3, Math.min(8, rowHeight - 5), alignments[column], column == 0, values[column]);
        }
    }

    private void drawReplacementMaterials(PDDocument document, List<QuotationItemResponse> items) throws IOException {
        fillWhite(document, 3, 75, 278, 390, 102);
        int visible = Math.min(items.size(), 7);
        for (int index = 0; index < visible; index++) {
            QuotationItemResponse item = items.get(index);
            BigDecimal rate = item.replacementRate() == null ? BigDecimal.ZERO : item.replacementRate();
            String text = item.itemNameSnapshot() + " - Rs. " + rate.stripTrailingZeros().toPlainString() + "/- per " + unit(item.unitSnapshot()).toLowerCase();
            drawFittedText(document, 3, 82, 282 + index * 13f, 375, 11, 8f, "LEFT", false, text);
        }
        if (items.size() > visible) {
            drawFittedText(document, 3, 82, 282 + 6 * 13f, 375, 11, 7.5f, "LEFT", true,
                    "Additional replacement rates are listed in the material schedule annexure.");
        }
    }

    private void fillWhite(PDDocument document, int pageIndex, float x, float top, float width, float height) throws IOException {
        PDPage page = document.getPage(pageIndex);
        float y = page.getMediaBox().getHeight() - top - height;
        try (PDPageContentStream stream = new PDPageContentStream(document, page,
                PDPageContentStream.AppendMode.APPEND, true, true)) {
            stream.setNonStrokingColor(255, 255, 255);
            stream.addRect(x, y, width, height);
            stream.fill();
        }
    }

    private void drawGrid(PDDocument document, int pageIndex, float x, float top, float[] widths,
            float rowHeight, int rows, float headerHeight) throws IOException {
        PDPage page = document.getPage(pageIndex);
        float totalHeight = headerHeight + rows * rowHeight;
        float yTop = page.getMediaBox().getHeight() - top;
        try (PDPageContentStream stream = new PDPageContentStream(document, page,
                PDPageContentStream.AppendMode.APPEND, true, true)) {
            stream.setStrokingColor(45, 45, 45);
            stream.setLineWidth(.45f);
            stream.addRect(x, yTop - totalHeight, sum(widths), totalHeight);
            float currentX = x;
            for (int column = 0; column < widths.length - 1; column++) {
                currentX += widths[column]; stream.moveTo(currentX, yTop); stream.lineTo(currentX, yTop - totalHeight);
            }
            if (headerHeight > 0) { stream.moveTo(x, yTop - headerHeight); stream.lineTo(x + sum(widths), yTop - headerHeight); }
            for (int row = 1; row < rows; row++) {
                float y = yTop - headerHeight - row * rowHeight; stream.moveTo(x, y); stream.lineTo(x + sum(widths), y);
            }
            stream.stroke();
        }
    }

    private void drawFittedText(PDDocument document, int pageIndex, float x, float top, float width, float height,
            float preferredSize, String align, boolean bold, String rawValue) throws IOException {
        if (rawValue == null || rawValue.isBlank()) return;
        PDFont font = bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;
        String value = rawValue.trim(); float size = preferredSize;
        while (size > 5.5f && font.getStringWidth(value) / 1000f * size > width) size -= .5f;
        while (font.getStringWidth(value) / 1000f * size > width && value.length() > 4) value = value.substring(0, value.length() - 4) + "...";
        PDPage page = document.getPage(pageIndex);
        float textWidth = font.getStringWidth(value) / 1000f * size;
        float textX = switch (align) {
            case "RIGHT" -> x + width - textWidth;
            case "CENTER" -> x + (width - textWidth) / 2f;
            default -> x;
        };
        try (PDPageContentStream stream = new PDPageContentStream(document, page,
                PDPageContentStream.AppendMode.APPEND, true, true)) {
            stream.setNonStrokingColor(0, 0, 0); stream.beginText(); stream.setFont(font, size);
            stream.newLineAtOffset(textX, page.getMediaBox().getHeight() - top - Math.min(size, height));
            stream.showText(value); stream.endText();
        }
    }

    private float before(float[] values, int index) { float total = 0; for (int i = 0; i < index; i++) total += values[i]; return total; }
    private float sum(float[] values) { float total = 0; for (float value : values) total += value; return total; }

    private void drawArea(PDDocument document, FieldArea area, String value) throws IOException {
        PDPage page = document.getPage(area.page() - 1);
        float pageHeight = page.getMediaBox().getHeight();
        float y = pageHeight - area.top() - area.height();
        PDFont font = Boolean.TRUE.equals(area.bold()) ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;
        try (PDPageContentStream stream = new PDPageContentStream(document, page,
                PDPageContentStream.AppendMode.APPEND, true, true)) {
            stream.setNonStrokingColor(255, 255, 255);
            stream.addRect(area.x(), y, area.width(), area.height());
            stream.fill();
            stream.setNonStrokingColor(0, 0, 0);
            float fontSize = area.fontSize();
            List<String> lines = fittedLines(font, area, value, fontSize);
            float lineHeight = fontSize * 1.2f;
            while ((lines.size() * lineHeight > area.height() + 1 || widest(font, fontSize, lines) > area.width() + .5f)
                    && fontSize > 6f) {
                fontSize -= .5f;
                lines = fittedLines(font, area, value, fontSize);
                lineHeight = fontSize * 1.2f;
            }
            if (lines.size() * lineHeight > area.height() + 1 || widest(font, fontSize, lines) > area.width() + .5f) overflow(value);
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                float textWidth = font.getStringWidth(line) / 1000f * fontSize;
                float x = switch (value(area.align(), "LEFT")) {
                    case "RIGHT" -> area.x() + area.width() - textWidth;
                    case "CENTER" -> area.x() + (area.width() - textWidth) / 2f;
                    default -> area.x();
                };
                stream.beginText();
                stream.setFont(font, fontSize);
                stream.newLineAtOffset(x, pageHeight - area.top() - fontSize - index * lineHeight);
                stream.showText(line);
                stream.endText();
            }
        }
    }

    private List<String> fittedLines(PDFont font, FieldArea area, String value, float fontSize) throws IOException {
        return Boolean.TRUE.equals(area.wrap()) ? wrap(font, fontSize, area.width(), value) : List.of(value);
    }

    private float widest(PDFont font, float fontSize, List<String> lines) throws IOException {
        float widest = 0;
        for (String line : lines) widest = Math.max(widest, font.getStringWidth(line) / 1000f * fontSize);
        return widest;
    }

    private List<String> wrap(PDFont font, float size, float width, String value) throws IOException {
        List<String> lines = new ArrayList<>(); StringBuilder line = new StringBuilder();
        for (String word : value.trim().split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.getStringWidth(candidate) / 1000f * size <= width) line.replace(0, line.length(), candidate);
            else { if (line.isEmpty()) overflow(word); lines.add(line.toString()); line.replace(0, line.length(), word); }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    private void requireExactTemplate(QuotationResponse quotation) {
        if (!TEMPLATE_CODE.equals(quotation.quotationTemplateCode()))
            throw new BusinessRuleException("WRONG_PDF_TEMPLATE", "Quotation does not use the SteelFab exact hire template");
    }
    private void overflow(String value) { throw new BusinessRuleException("EXACT_PDF_TEXT_OVERFLOW", "Value is too long for the exact PDF: " + value); }
    private String unit(String unit) { return unit == null || unit.isBlank() ? "Nos." : unit; }
    private String value(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
    private String labeled(String label, String value) { return label + " " + (value == null || value.isBlank() ? "____________________________" : value.trim()); }
    private String filename(QuotationResponse q) { return "steelfab-hire-" + q.quotationNumber().replaceAll("[^A-Za-z0-9.-]", "-") + ".pdf"; }

    public record PdfDocument(String filename, byte[] content, int templateVersion, int coordinatesVersion) {}
    public record ExactPdfConfig(String templateCode, int templateVersion, int coordinatesVersion, int pageCount,
            float pageHeight, Map<String, List<FieldArea>> fields) {}
    public record FieldArea(int page, float x, float top, float width, float height, float fontSize,
            String align, Boolean bold, Boolean wrap, Integer rows, Float rowHeight) {}
}
