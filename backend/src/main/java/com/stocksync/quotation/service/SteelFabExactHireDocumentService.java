package com.stocksync.quotation.service;

import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.file.entity.FileAttachment;
import com.stocksync.file.service.FileStorageService;
import com.stocksync.quotation.entity.Quotation;
import com.stocksync.quotation.entity.QuotationStatus;
import com.stocksync.quotation.repository.QuotationRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SteelFabExactHireDocumentService {
    private final QuotationRepository repository;
    private final QuotationService quotations;
    private final SteelFabExactHirePdfService renderer;
    private final FileStorageService files;

    public SteelFabExactHireDocumentService(QuotationRepository repository,QuotationService quotations,
            SteelFabExactHirePdfService renderer,FileStorageService files){
        this.repository=repository;this.quotations=quotations;this.renderer=renderer;this.files=files;
    }

    public SteelFabExactHirePdfService.PdfDocument preview(Long id){return renderer.generate(quotations.get(id));}

    @Transactional
    public SteelFabExactHirePdfService.PdfDocument finalizeDocument(Long id,HttpServletRequest request){
        Quotation quotation=repository.findDetailedForUpdate(id).orElseThrow(()->new BusinessRuleException("QUOTATION_NOT_FOUND","Quotation not found"));
        if(!SteelFabExactHirePdfService.TEMPLATE_CODE.equals(quotation.getQuotationTemplate().getTemplateCode()))
            throw new BusinessRuleException("WRONG_PDF_TEMPLATE","Quotation does not use the SteelFab exact hire template");
        if(quotation.getStatus()!=QuotationStatus.APPROVED&&quotation.getStatus()!=QuotationStatus.CONVERTED)
            throw new BusinessRuleException("QUOTATION_NOT_APPROVED","Approve the quotation before finalizing its exact PDF");
        if(quotation.getExactPdfAttachment()!=null){
            return new SteelFabExactHirePdfService.PdfDocument(quotation.getExactPdfAttachment().getOriginalFilename(),
                    files.read(quotation.getExactPdfAttachment().getId()),quotation.getExactPdfTemplateVersion(),quotation.getExactPdfCoordinatesVersion());
        }
        var generated=renderer.generate(quotations.get(id));
        FileAttachment attachment=files.storeGenerated("QUOTATION",id,"FINAL_EXACT_HIRE_PDF",generated.filename(),generated.content(),
                "Immutable SteelFab exact hire quotation and agreement");
        quotation.setExactPdfAttachment(attachment);quotation.setExactPdfTemplateCode(SteelFabExactHirePdfService.TEMPLATE_CODE);
        quotation.setExactPdfTemplateVersion(generated.templateVersion());quotation.setExactPdfCoordinatesVersion(generated.coordinatesVersion());
        quotation.setExactPdfChecksumSha256(sha256(generated.content()));quotation.setExactPdfFinalizedAt(Instant.now());quotation.setExactPdfFinalizedBy(actor());
        quotation.setUpdatedBy(actor());repository.save(quotation);
        return generated;
    }

    private String sha256(byte[] content){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 is unavailable",e);}}
    private String actor(){var auth=SecurityContextHolder.getContext().getAuthentication();return auth==null?"system":auth.getName();}
}
