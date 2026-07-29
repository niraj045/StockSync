package com.stocksync.agreement.service;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.stocksync.common.pdf.PdfViewHelper;
import com.stocksync.agreement.dto.AgreementResponse;
import java.io.*;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
@Service
public class AgreementPdfService {
 private final SpringTemplateEngine templates;
 public AgreementPdfService(SpringTemplateEngine templates){this.templates=templates;}
 public byte[] generate(AgreementResponse agreement){
  Context c=new Context();c.setVariables(Map.of("a",agreement,"fmt",PdfViewHelper.INSTANCE));
  String view="rocks-logs-v1".equals(agreement.templateLayoutKey())?"agreement-rocks-logs-pdf":"agreement-pdf";
  try(ByteArrayOutputStream out=new ByteArrayOutputStream()){
   new PdfRendererBuilder().withHtmlContent(templates.process(view,c),null).toStream(out).run();return out.toByteArray();
  }catch(IOException e){throw new IllegalStateException("Unable to generate agreement PDF",e);}
 }
}
