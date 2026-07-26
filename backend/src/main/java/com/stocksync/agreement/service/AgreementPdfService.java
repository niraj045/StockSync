package com.stocksync.agreement.service;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
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
  Context c=new Context();c.setVariables(Map.of("a",agreement));
  try(ByteArrayOutputStream out=new ByteArrayOutputStream()){
   new PdfRendererBuilder().withHtmlContent(templates.process("agreement-pdf",c),null).toStream(out).run();return out.toByteArray();
  }catch(IOException e){throw new IllegalStateException("Unable to generate agreement PDF",e);}
 }
}
