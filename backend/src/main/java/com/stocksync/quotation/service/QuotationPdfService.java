package com.stocksync.quotation.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.stocksync.common.pdf.PdfViewHelper;
import com.stocksync.quotation.dto.QuotationResponse;
import com.stocksync.file.service.FileStorageService;
import java.io.*;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class QuotationPdfService {
    private final QuotationService quotations;
    private final SpringTemplateEngine templates;
    private final SteelFabExactHirePdfService exactPdf; private final FileStorageService files;
    public QuotationPdfService(QuotationService quotations,SpringTemplateEngine templates,SteelFabExactHirePdfService exactPdf,FileStorageService files){this.quotations=quotations;this.templates=templates;this.exactPdf=exactPdf;this.files=files;}
    public PdfDocument generate(Long id){
        QuotationResponse quotation=quotations.get(id);
        if(SteelFabExactHirePdfService.TEMPLATE_CODE.equals(quotation.quotationTemplateCode())){
            if(quotation.exactPdfAttachmentId()!=null&&Objects.equals(quotation.exactPdfCoordinatesVersion(),exactPdf.coordinatesVersion()))
                return new PdfDocument("steelfab-hire-"+quotation.quotationNumber().replaceAll("[^A-Za-z0-9.-]","-")+".pdf",files.read(quotation.exactPdfAttachmentId()));
            var exact=exactPdf.generate(quotation);return new PdfDocument(exact.filename(),exact.content());
        }
        Context context=new Context();context.setVariables(Map.of("q",quotation,"fmt",PdfViewHelper.INSTANCE));
        String html=templates.process("quotation-pdf",context);
        return render(quotation,html);
    }
    PdfDocument render(QuotationResponse quotation,String html){
        try(ByteArrayOutputStream output=new ByteArrayOutputStream()){
            new PdfRendererBuilder().useFastMode().withHtmlContent(html, null).toStream(output).run();
            return new PdfDocument(("quotation-"+quotation.quotationNumber().replaceAll("[^A-Za-z0-9.-]","-")+".pdf"),output.toByteArray());
        }catch(IOException e){throw new IllegalStateException("Unable to generate quotation PDF",e);}
    }
    public record PdfDocument(String filename,byte[] content){}
}
