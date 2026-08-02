package com.stocksync.agreement.service;

import com.stocksync.agreement.dto.*;
import com.stocksync.agreement.entity.*;
import com.stocksync.agreement.repository.AgreementRepository;
import com.stocksync.agreement.repository.AgreementTemplateRepository;
import com.stocksync.audit.service.UserActivityLogService;
import com.stocksync.auth.entity.User;
import com.stocksync.auth.repository.UserRepository;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.common.numbering.*;
import com.stocksync.file.entity.FileAttachment;
import com.stocksync.file.repository.FileAttachmentRepository;
import com.stocksync.inventory.repository.ItemRepository;
import com.stocksync.quotation.entity.*;
import com.stocksync.quotation.repository.QuotationRepository;
import com.stocksync.quotation.service.QuotationService;
import com.stocksync.quotation.service.SteelFabExactHirePdfService;
import com.stocksync.file.service.FileStorageService;
import com.stocksync.site.entity.SiteStatus;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.data.domain.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgreementService implements AgreementAccess {
 private final AgreementRepository agreements; private final QuotationRepository quotations; private final ItemRepository items;
 private final AgreementTemplateRepository templates;
 private final FileAttachmentRepository attachments; private final DocumentNumberService numbers; private final AgreementPdfService pdf;
 private final UserRepository users; private final UserActivityLogService audit; private final Path storageRoot;
 private final SteelFabExactHirePdfService exactPdf; private final QuotationService quotationService; private final FileStorageService fileStorage;
 public AgreementService(AgreementRepository agreements,QuotationRepository quotations,ItemRepository items,AgreementTemplateRepository templates,
   FileAttachmentRepository attachments,DocumentNumberService numbers,AgreementPdfService pdf,UserRepository users,
   UserActivityLogService audit,@Value("${stocksync.file-storage-path}")String root,SteelFabExactHirePdfService exactPdf,
   QuotationService quotationService,FileStorageService fileStorage){
  this.agreements=agreements;this.quotations=quotations;this.items=items;this.templates=templates;this.attachments=attachments;this.numbers=numbers;
  this.pdf=pdf;this.users=users;this.audit=audit;this.storageRoot=Paths.get(root).toAbsolutePath().normalize();
  this.exactPdf=exactPdf;this.quotationService=quotationService;this.fileStorage=fileStorage;
 }

 @Transactional(readOnly=true)
 public Page<AgreementResponse> list(String search,String agreementNumber,Long quotationId,Long partyId,Long siteId,
   AgreementStatus status,LocalDate effectiveFrom,LocalDate effectiveTo,LocalDate expiryFrom,LocalDate expiryTo,Pageable pageable){
  return agreements.findAll((root,q,cb)->{List<Predicate> p=new ArrayList<>();
   if(search!=null&&!search.isBlank()){String s="%"+search.trim().toLowerCase(Locale.ROOT)+"%";p.add(cb.or(
    cb.like(cb.lower(root.get("agreementNumber")),s),cb.like(cb.lower(root.get("partyLegalNameSnapshot")),s),cb.like(cb.lower(root.get("siteNameSnapshot")),s)));}
   if(agreementNumber!=null&&!agreementNumber.isBlank())p.add(cb.equal(root.get("agreementNumber"),agreementNumber));
   if(quotationId!=null)p.add(cb.equal(root.get("quotation").get("id"),quotationId));if(partyId!=null)p.add(cb.equal(root.get("party").get("id"),partyId));
   if(siteId!=null)p.add(cb.equal(root.get("site").get("id"),siteId));if(status!=null)p.add(cb.equal(root.get("status"),status));
   if(effectiveFrom!=null)p.add(cb.greaterThanOrEqualTo(root.get("effectiveDate"),effectiveFrom));if(effectiveTo!=null)p.add(cb.lessThanOrEqualTo(root.get("effectiveDate"),effectiveTo));
   if(expiryFrom!=null)p.add(cb.greaterThanOrEqualTo(root.get("expiryDate"),expiryFrom));if(expiryTo!=null)p.add(cb.lessThanOrEqualTo(root.get("expiryDate"),expiryTo));
   return cb.and(p.toArray(Predicate[]::new));},pageable).map(this::response);
 }
 @Transactional(readOnly=true) public AgreementResponse get(Long id){return response(require(id));}

 @Transactional
 public AgreementResponse convert(Long quotationId,QuotationConversionRequest request,HttpServletRequest http){
  Optional<Agreement> existing=agreements.findByQuotationId(quotationId);if(existing.isPresent())return response(require(existing.get().getId()));
  Quotation q=quotations.findDetailedForUpdate(quotationId).orElseThrow(()->error("QUOTATION_NOT_FOUND","Quotation not found"));
  existing=agreements.findByQuotationId(quotationId);if(existing.isPresent())return response(require(existing.get().getId()));
  if(q.getStatus()!=QuotationStatus.APPROVED)throw error("QUOTATION_NOT_APPROVED","Only an approved quotation can be converted");
  if(q.getItems().isEmpty())throw error("AGREEMENT_ITEMS_REQUIRED","Quotation must contain at least one item");
  if(q.getSite().getStatus()==SiteStatus.CLOSED)throw error("SITE_CLOSED","Closed site cannot receive an agreement");
  if(!q.getSite().getParty().getId().equals(q.getParty().getId()))throw error("SITE_PARTY_MISMATCH","Site does not belong to quotation party");
  LocalDate today=LocalDate.now(); Agreement a=new Agreement();a.setAgreementNumber(numbers.next(DocumentType.AGREEMENT,today));
  if(request!=null&&request.templateId()!=null){
   AgreementTemplate template=templates.findById(request.templateId()).filter(AgreementTemplate::isActive)
    .orElseThrow(()->error("AGREEMENT_TEMPLATE_NOT_FOUND_OR_INACTIVE","Active agreement template not found"));
   a.setTemplate(template);
  }
  LocalDate effective=request==null?today:request.effectiveDate();
  a.setQuotation(q);a.setParty(q.getParty());a.setSite(q.getSite());a.setAgreementDate(today);a.setEffectiveDate(effective);a.setExpiryDate(request==null?null:request.expiryDate());a.setRentalType(q.getRentalType());
  a.setBillingCycle(BillingCycle.MONTHLY);a.setStatus(AgreementStatus.DRAFT);copySnapshots(a,q);copyCommercials(a,q);
  if(request!=null){a.setSecurityDeposit(request.securityDeposit());a.setNotes(trim(request.notes()));}
  List<AgreementItem> lines=new ArrayList<>();for(QuotationItem source:q.getItems()){AgreementItem i=new AgreementItem();
   i.setSourceQuotationItem(source);i.setItem(source.getItem());i.setItemCodeSnapshot(source.getItemCodeSnapshot());i.setItemNameSnapshot(source.getItemNameSnapshot());
   i.setDescriptionSnapshot(source.getDescriptionSnapshot());i.setSizeSnapshot(source.getSizeSnapshot());i.setUnitSnapshot(source.getUnitSnapshot());
   i.setWeightSnapshot(source.getWeight());i.setAgreedQuantity(source.getQuantity());i.setUnitRate(source.getUnitRate());
   i.setRentalRate(source.getRentalRate());i.setRentalType(source.getRentalType());i.setSequence(source.getSequence());i.setNotes(source.getNotes());
   i.setArea(source.getArea()); lines.add(i);
  }a.replaceItems(lines);a.setCreatedBy(actor());a.setUpdatedBy(actor());Agreement saved=agreements.save(a);q.setStatus(QuotationStatus.CONVERTED);
  log("AGREEMENT_CREATED_FROM_QUOTATION",saved,"Source quotation "+q.getQuotationNumber(),http);
  log("QUOTATION_CONVERTED_TO_AGREEMENT",saved,"Agreement "+saved.getAgreementNumber(),http);return response(saved);
 }

 @Transactional public AgreementResponse convert(Long quotationId,HttpServletRequest http){
  return convert(quotationId,null,http);
 }

 @Transactional public AgreementResponse update(Long id,AgreementRequest r,HttpServletRequest http){
  Agreement a=require(id);expect(a,AgreementStatus.DRAFT);if(a.getVersion()!=r.version())throw new ObjectOptimisticLockingFailureException(Agreement.class,id);
  validate(r);a.setAgreementDate(r.agreementDate());a.setEffectiveDate(r.effectiveDate());a.setExpiryDate(r.expiryDate());a.setBillingCycle(r.billingCycle());
  a.setCustomBillingCycleDays(r.customBillingCycleDays());a.setGracePeriodDays(zero(r.gracePeriodDays()));a.setMinimumBillingDays(zero(r.minimumBillingDays()));
  a.setSecurityDeposit(r.securityDeposit());a.setTerms(trim(r.terms()));a.setNotes(trim(r.notes()));
  if(r.billingStartRule()!=null)a.setBillingStartRule(BillingStartRule.valueOf(r.billingStartRule()));
  if(r.billingEndRule()!=null)a.setBillingEndRule(BillingEndRule.valueOf(r.billingEndRule()));
  a.setGeneratedDocument(null);a.setGeneratedFilename(null);a.setGeneratedStoragePath(null);a.setGeneratedAt(null);
  Map<Long,AgreementItem> originals=new HashMap<>();for(AgreementItem i:a.getItems())originals.put(i.getItem().getId(),i);
  List<AgreementItem> lines=new ArrayList<>();Set<Long> unique=new HashSet<>();for(AgreementItemRequest v:r.items()){
   if(!unique.add(v.itemId()))throw error("DUPLICATE_ITEM_LINE","Each item may appear only once");
   AgreementItem i=originals.get(v.itemId());if(i==null)throw error("AGREEMENT_ITEM_NOT_FROM_QUOTATION","Agreement items must originate from the quotation");
   i.setAgreedQuantity(v.contractedQuantity());i.setUnitRate(v.rate());i.setRentalRate(v.rate());i.setAreaRate(v.areaRate());i.setWeightRate(v.weightRate());
   i.setLossRatePerPiece(v.lossRatePerPiece());i.setLossRatePerWeight(v.lossRatePerWeight());i.setDamageRate(v.damageRate());i.setSequence(zero(v.sequence()));i.setNotes(trim(v.notes()));
   List<AgreementItemSlab> slabsList = new ArrayList<>();
   if(v.slabs() != null) {
       for(AgreementItemSlabRequest slabReq : v.slabs()) {
           AgreementItemSlab slab = new AgreementItemSlab();
           slab.setStartDay(slabReq.startDay());
           slab.setEndDay(slabReq.endDay());
           slab.setRate(slabReq.rate());
           slab.setAgreementItem(i);
           slabsList.add(slab);
       }
   }
   i.replaceSlabs(slabsList);
   lines.add(i);
  }a.replaceItems(lines);a.setUpdatedBy(actor());Agreement saved=agreements.save(a);log("AGREEMENT_UPDATED",saved,"Draft updated",http);return response(saved);
 }
 @Transactional public AgreementResponse ready(Long id,HttpServletRequest h){Agreement a=require(id);expect(a,AgreementStatus.DRAFT);validateReady(a);a.setStatus(AgreementStatus.READY_FOR_REVIEW);a.setReadyForReviewAt(Instant.now());a.setReadyForReviewBy(actor());return saveLog(a,"AGREEMENT_READY_FOR_REVIEW","Ready for review",h);}
 @Transactional public AgreementResponse returnToDraft(Long id,AgreementReasonRequest r,HttpServletRequest h){Agreement a=require(id);expect(a,AgreementStatus.READY_FOR_REVIEW);a.setStatus(AgreementStatus.DRAFT);a.setNotes(join(a.getNotes(),"Returned: "+r.reason().trim()));return saveLog(a,"AGREEMENT_RETURNED_TO_DRAFT",r.reason(),h);}
 @Transactional public AgreementResponse generate(Long id,HttpServletRequest h){Agreement a=require(id);if(a.getStatus()!=AgreementStatus.DRAFT&&a.getStatus()!=AgreementStatus.READY_FOR_REVIEW)throw transition(a);
  boolean exact=a.getQuotation()!=null&&a.getQuotation().getQuotationTemplate()!=null&&SteelFabExactHirePdfService.TEMPLATE_CODE.equals(a.getQuotation().getQuotationTemplate().getTemplateCode());
  byte[] bytes=exact?(a.getQuotation().getExactPdfAttachment()!=null?fileStorage.read(a.getQuotation().getExactPdfAttachment().getId()):exactPdf.generate(quotationService.get(a.getQuotation().getId())).content()):pdf.generate(response(a));
  String safe=(exact?"steelfab-hire-agreement-":"agreement-")+a.getAgreementNumber().replaceAll("[^A-Za-z0-9.-]","-")+".pdf";
  Path dir=storageRoot.resolve("agreements").resolve(String.valueOf(a.getId())).normalize();Path target=dir.resolve(UUID.randomUUID()+".pdf").normalize();
  if(!target.startsWith(storageRoot))throw error("INVALID_FILE_PATH","Invalid agreement document path");
  try{Files.createDirectories(dir);Files.write(target,bytes);}catch(Exception e){try{Files.deleteIfExists(target);}catch(Exception ignored){}throw error("AGREEMENT_GENERATION_FAILED","Unable to generate agreement document");}
  FileAttachment f=new FileAttachment();f.setEntityType("AGREEMENT");f.setEntityId(a.getId());f.setDocumentType("AGREEMENT_PDF");f.setOriginalFilename(safe);
  f.setStoredFilename(target.getFileName().toString());f.setContentType("application/pdf");f.setFileSize(bytes.length);f.setStoragePath(storageRoot.relativize(target).toString());
  f.setDescription("Generated agreement PDF");f.setUploadedBy(actor());f=attachments.save(f);a.setGeneratedDocument(f);a.setGeneratedFilename(safe);a.setGeneratedStoragePath(f.getStoragePath());a.setGeneratedAt(Instant.now());
  return saveLog(a,"AGREEMENT_DOCUMENT_GENERATED",safe,h);
 }
 @Transactional public AgreementResponse activate(Long id,HttpServletRequest h){Agreement a=require(id);expect(a,AgreementStatus.READY_FOR_REVIEW);validateReady(a);
  if(a.getGeneratedDocument()==null)throw error("AGREEMENT_DOCUMENT_REQUIRED","Generate the agreement document before activation");
  if(agreements.existsBySiteIdAndStatusAndIdNot(a.getSite().getId(),AgreementStatus.ACTIVE,a.getId()))throw error("ACTIVE_AGREEMENT_ALREADY_EXISTS","An active agreement already exists for this site");
  a.setStatus(AgreementStatus.ACTIVE);a.setActivatedAt(Instant.now());a.setActivatedBy(actor());return saveLog(a,"AGREEMENT_ACTIVATED","Agreement activated",h);}
 @Transactional public AgreementResponse expire(Long id,HttpServletRequest h){Agreement a=require(id);expect(a,AgreementStatus.ACTIVE);a.setStatus(AgreementStatus.EXPIRED);a.setExpiredAt(Instant.now());a.setExpiredBy(actor());return saveLog(a,"AGREEMENT_EXPIRED","Agreement expired",h);}
 @Transactional public AgreementResponse terminate(Long id,AgreementReasonRequest r,HttpServletRequest h){Agreement a=require(id);expect(a,AgreementStatus.ACTIVE);a.setStatus(AgreementStatus.TERMINATED);a.setTerminationReason(r.reason().trim());a.setTerminatedAt(Instant.now());a.setTerminatedBy(actor());return saveLog(a,"AGREEMENT_TERMINATED",r.reason(),h);}
 @Transactional public AgreementResponse close(Long id,HttpServletRequest h){Agreement a=require(id);if(a.getStatus()!=AgreementStatus.ACTIVE&&a.getStatus()!=AgreementStatus.EXPIRED)throw transition(a);a.setStatus(AgreementStatus.CLOSED);a.setClosedAt(Instant.now());a.setClosedBy(actor());return saveLog(a,"AGREEMENT_CLOSED","Normal completion",h);}
 @Transactional public AgreementResponse cancel(Long id,AgreementReasonRequest r,HttpServletRequest h){Agreement a=require(id);if(a.getStatus()!=AgreementStatus.DRAFT&&a.getStatus()!=AgreementStatus.READY_FOR_REVIEW)throw transition(a);a.setStatus(AgreementStatus.CANCELLED);a.setCancellationReason(r.reason().trim());a.setCancelledAt(Instant.now());a.setCancelledBy(actor());return saveLog(a,"AGREEMENT_CANCELLED",r.reason(),h);}

 @Transactional(readOnly=true) public Download download(Long id){Agreement a=require(id);FileAttachment f=a.getGeneratedDocument();if(f==null)throw error("AGREEMENT_DOCUMENT_REQUIRED","Agreement document has not been generated");
  Path path=storageRoot.resolve(f.getStoragePath()).normalize();if(!path.startsWith(storageRoot)||!Files.isRegularFile(path))throw error("FILE_NOT_FOUND","Agreement document not found");
  return new Download(new FileSystemResource(path),f.getOriginalFilename(),f.getContentType());
 }
 @Override @Transactional(readOnly=true) public OrderAgreement requireForOrder(Long id){Agreement a=require(id);if(a.getStatus()!=AgreementStatus.ACTIVE||a.getExpiryDate()!=null&&a.getExpiryDate().isBefore(LocalDate.now()))throw error("AGREEMENT_NOT_ORDERABLE","Agreement is not active");
  return new OrderAgreement(a.getId(),a.getParty().getId(),a.getSite().getId(),a.getStatus(),a.getItems().stream().map(i->new OrderAgreementItem(i.getItem().getId(),i.getAgreedQuantity())).toList());}

 private void validate(AgreementRequest r){if(r.expiryDate()!=null&&r.expiryDate().isBefore(r.effectiveDate()))throw error("INVALID_AGREEMENT_DATES","Expiry cannot precede effective date");
  if(r.expiryDate()!=null&&r.agreementDate().isAfter(r.expiryDate()))throw error("INVALID_AGREEMENT_DATES","Agreement date cannot be after expiry date");
  if(r.billingCycle()==BillingCycle.CUSTOM&&(r.customBillingCycleDays()==null||r.customBillingCycleDays()<=0))throw error("INVALID_BILLING_CYCLE","Custom billing cycle requires positive days");}
 private void validateReady(Agreement a){if(a.getEffectiveDate()==null||a.getItems().isEmpty())throw error("AGREEMENT_NOT_READY","Effective date and agreement items are required");}
 private void copySnapshots(Agreement a,Quotation q){var p=q.getParty();var s=q.getSite();a.setPartyLegalNameSnapshot(q.getPartyNameSnapshot());a.setPartyTradeNameSnapshot(p.getTradeName());a.setPartyGstinSnapshot(p.getGstin());a.setPartyPanSnapshot(p.getPan());a.setPartyAddressSnapshot(p.getAddress());a.setPartyStateSnapshot(p.getState());a.setPartyContactSnapshot(String.join(" / ",nonNull(p.getContactPerson()),nonNull(p.getPhone()),nonNull(p.getEmail())));a.setSiteNameSnapshot(q.getSiteNameSnapshot());a.setSiteCodeSnapshot(s.getSiteCode());a.setSiteAddressSnapshot(s.getAddress());a.setSiteContactSnapshot(s.getContactPerson());a.setQuotationNumberSnapshot(q.getQuotationNumber());a.setQuotationDateSnapshot(q.getQuotationDate());a.setQuotationApprovedAtSnapshot(q.getApprovedAt());}
 private void copyCommercials(Agreement a,Quotation q){a.setSecurityDeposit(q.getSecurityDeposit());a.setSubtotal(q.getSubtotal());a.setDiscountAmount(q.getDiscountAmount());a.setTaxableAmount(q.getTaxableAmount());a.setCgstAmount(q.getCgstAmount());a.setSgstAmount(q.getSgstAmount());a.setIgstAmount(q.getIgstAmount());a.setTotalTax(q.getTotalTax());a.setTransportCharge(q.getTransportCharge());a.setLoadingCharge(q.getLoadingCharge());a.setUnloadingCharge(q.getUnloadingCharge());a.setOtherCharge(q.getOtherCharge());a.setRoundOff(q.getRoundOff());a.setGrandTotal(q.getGrandTotal());a.setTerms(q.getTerms());a.setNotes(q.getNotes());}
 private Agreement require(Long id){return agreements.findDetailedById(id).orElseThrow(()->error("AGREEMENT_NOT_FOUND","Agreement not found"));}
 private void expect(Agreement a,AgreementStatus status){if(a.getStatus()!=status)throw transition(a);} private BusinessRuleException transition(Agreement a){return error("INVALID_AGREEMENT_STATUS_TRANSITION","Operation is not allowed while agreement is "+a.getStatus());}
 private AgreementResponse saveLog(Agreement a,String action,String description,HttpServletRequest h){a.setUpdatedBy(actor());Agreement saved=agreements.save(a);log(action,saved,description,h);return response(saved);}
 private AgreementItemResponse itemResponse(AgreementItem i) {
    return new AgreementItemResponse(
        i.getId(),
        i.getSourceQuotationItem() == null ? null : i.getSourceQuotationItem().getId(),
        i.getItem().getId(),
        i.getItemCodeSnapshot(),
        i.getItemNameSnapshot(),
        i.getDescriptionSnapshot(),
        i.getSizeSnapshot(),
        i.getUnitSnapshot(),
        i.getWeightSnapshot(),
        i.getAgreedQuantity(),
        i.getUnitRate(),
        i.getRentalType(),
        i.getAreaRate(),
        i.getWeightRate(),
        i.getLossRatePerPiece(),
        i.getLossRatePerWeight(),
        i.getDamageRate(),
        i.getSequence(),
        i.getNotes(),
        i.getArea(),
        i.getSlabs() == null ? Collections.emptyList() : i.getSlabs().stream().map(s -> new AgreementItemSlabResponse(s.getId(), s.getStartDay(), s.getEndDay(), s.getRate())).toList()
    );
  }
 private AgreementResponse response(Agreement a){AgreementTemplate template=a.getTemplate();return new AgreementResponse(a.getId(),a.getAgreementNumber(),a.getQuotation()==null?null:a.getQuotation().getId(),a.getQuotationNumberSnapshot(),a.getQuotationDateSnapshot(),
  template==null?null:template.getId(),template==null?null:template.getTemplateCode(),template==null?null:template.getName(),template==null?null:template.getLayoutKey(),template==null?null:template.getTemplateVersion(),
  a.getParty().getId(),a.getPartyLegalNameSnapshot(),a.getPartyTradeNameSnapshot(),a.getPartyGstinSnapshot(),a.getPartyPanSnapshot(),a.getPartyAddressSnapshot(),a.getPartyStateSnapshot(),a.getPartyContactSnapshot(),
  a.getSite().getId(),a.getSiteNameSnapshot(),a.getSiteCodeSnapshot(),a.getSiteAddressSnapshot(),a.getSiteContactSnapshot(),a.getAgreementDate(),a.getEffectiveDate(),a.getExpiryDate(),a.getRentalType(),a.getBillingCycle(),
  a.getCustomBillingCycleDays(),a.getGracePeriodDays(),a.getMinimumBillingDays(),a.getStatus(),a.getSecurityDeposit(),a.getSubtotal(),a.getDiscountAmount(),a.getTaxableAmount(),a.getCgstAmount(),a.getSgstAmount(),a.getIgstAmount(),a.getTotalTax(),
  a.getTransportCharge(),a.getLoadingCharge(),a.getUnloadingCharge(),a.getOtherCharge(),a.getRoundOff(),a.getGrandTotal(),a.getTerms(),a.getNotes(),
  a.getGeneratedDocument()==null?null:a.getGeneratedDocument().getId(),a.getGeneratedFilename(),a.getGeneratedAt(),a.getReadyForReviewAt(),a.getReadyForReviewBy(),a.getActivatedAt(),a.getActivatedBy(),
  a.getTerminationReason(),a.getTerminatedAt(),a.getTerminatedBy(),a.getCancellationReason(),a.getCancelledAt(),a.getCancelledBy(),
  a.getItems().stream().map(this::itemResponse).toList(),
  a.getBillingStartRule() == null ? "ISSUE_DATE_INCLUDED" : a.getBillingStartRule().name(),
  a.getBillingEndRule() == null ? "RETURN_DATE_EXCLUDED" : a.getBillingEndRule().name(),
  a.getVersion(),a.getCreatedAt(),a.getCreatedBy(),a.getUpdatedAt(),a.getUpdatedBy());}
 private void log(String action,Agreement a,String description,HttpServletRequest req){User u=users.findByUsernameIgnoreCase(actor()).orElse(null);audit.log(u==null?null:u.getId(),actor(),action,"Agreement",String.valueOf(a.getId()),description,req);}
 private String actor(){var a=SecurityContextHolder.getContext().getAuthentication();return a==null?"system":a.getName();} private BusinessRuleException error(String c,String m){return new BusinessRuleException(c,m);}
 private String trim(String v){return v==null||v.isBlank()?null:v.trim();}private String join(String a,String b){return a==null?b:a+"\n"+b;}private String nonNull(String v){return v==null?"":v;}private int zero(Integer v){return v==null?0:v;}
 public record Download(Resource resource,String filename,String contentType){}
}
