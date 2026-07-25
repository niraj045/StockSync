package com.stocksync.agreement.service;

import com.stocksync.agreement.dto.*;
import com.stocksync.agreement.entity.*;
import com.stocksync.agreement.repository.*;
import com.stocksync.audit.service.UserActivityLogService;
import com.stocksync.auth.entity.User;
import com.stocksync.auth.repository.UserRepository;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.inventory.entity.Item;
import com.stocksync.inventory.repository.ItemRepository;
import com.stocksync.party.entity.Party;
import com.stocksync.party.repository.PartyRepository;
import com.stocksync.quotation.entity.Quotation;
import com.stocksync.quotation.repository.QuotationRepository;
import com.stocksync.quotation.service.QuotationAccess;
import com.stocksync.site.entity.*;
import com.stocksync.site.repository.SiteRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.data.domain.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AgreementService implements AgreementAccess {
    private static final Set<String> TEMPLATE_TYPES=Set.of("application/pdf","application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    private final AgreementRepository agreements;private final AgreementTemplateRepository templates;private final QuotationRepository quotationEntities;
    private final QuotationAccess quotations;private final PartyRepository parties;private final SiteRepository sites;private final ItemRepository items;
    private final UserRepository users;private final UserActivityLogService audit;private final Path storageRoot;
    public AgreementService(AgreementRepository agreements,AgreementTemplateRepository templates,QuotationRepository quotationEntities,
            QuotationAccess quotations,PartyRepository parties,SiteRepository sites,ItemRepository items,UserRepository users,
            UserActivityLogService audit,@Value("${stocksync.file-storage-path}")String storageRoot){
        this.agreements=agreements;this.templates=templates;this.quotationEntities=quotationEntities;this.quotations=quotations;this.parties=parties;
        this.sites=sites;this.items=items;this.users=users;this.audit=audit;this.storageRoot=Paths.get(storageRoot).toAbsolutePath().normalize();}
    @Transactional(readOnly=true)
    public Page<AgreementResponse> list(String search,AgreementStatus status,Long partyId,Long siteId,Pageable pageable){
        return agreements.findAll((root,q,cb)->{List<Predicate>p=new ArrayList<>();
            if(search!=null&&!search.isBlank()){String t="%"+search.trim().toLowerCase(Locale.ROOT)+"%";
                p.add(cb.or(cb.like(cb.lower(root.get("agreementNumber")),t),cb.like(cb.lower(root.get("party").get("legalName")),t),
                        cb.like(cb.lower(root.get("site").get("siteName")),t)));}
            if(status!=null)p.add(cb.equal(root.get("status"),status));if(partyId!=null)p.add(cb.equal(root.get("party").get("id"),partyId));
            if(siteId!=null)p.add(cb.equal(root.get("site").get("id"),siteId));return cb.and(p.toArray(Predicate[]::new));},pageable).map(this::response);}
    @Transactional(readOnly=true)public AgreementResponse get(Long id){return response(detailed(id));}
    @Transactional public AgreementResponse create(AgreementRequest r,HttpServletRequest http){
        Agreement a=new Agreement();a.setAgreementNumber(number());a.setStatus(AgreementStatus.DRAFT);apply(a,r);
        a.setCreatedBy(actor());a.setUpdatedBy(actor());Agreement saved=agreements.save(a);log("AGREEMENT_DRAFT_CREATED",saved,"Manual draft",http);return response(saved);}
    @Transactional public AgreementResponse update(Long id,AgreementRequest r,HttpServletRequest http){
        Agreement a=detailed(id);requireDraft(a);if(r.version()==null||a.getVersion()!=r.version())
            throw new ObjectOptimisticLockingFailureException(Agreement.class,id);a.getItems().clear();agreements.flush();apply(a,r);a.setUpdatedBy(actor());
        Agreement saved=agreements.save(a);log("AGREEMENT_DRAFT_UPDATED",saved,"Draft updated",http);return response(saved);}
    @Transactional public AgreementResponse convert(Long quotationId,QuotationConversionRequest r,HttpServletRequest http){
        if(agreements.existsByQuotationId(quotationId))throw new BusinessRuleException("QUOTATION_ALREADY_CONVERTED","Quotation already has an agreement");
        var q=quotations.requireApprovedForConversion(quotationId);Agreement a=new Agreement();a.setAgreementNumber(number());
        a.setQuotation(quotationEntities.getReferenceById(quotationId));a.setParty(parties.getReferenceById(q.partyId()));a.setSite(sites.getReferenceById(q.siteId()));
        a.setTemplate(template(r.templateId()));a.setEffectiveDate(r.effectiveDate());a.setExpiryDate(r.expiryDate());
        if(r.expiryDate()!=null&&r.expiryDate().isBefore(r.effectiveDate()))throw new BusinessRuleException("INVALID_AGREEMENT_DATES","Expiry cannot precede effective date");
        a.setRentalType(q.rentalType());a.setStatus(AgreementStatus.DRAFT);a.setSecurityDeposit(r.securityDeposit());
        a.setTransportCharge(q.transportCharge());a.setLoadingCharge(q.loadingCharge());a.setUnloadingCharge(q.unloadingCharge());
        a.setTerms(q.terms());a.setNotes(joinNotes(q.notes(),r.notes()));List<AgreementItem>lines=new ArrayList<>();
        for(var line:q.items()){AgreementItem i=new AgreementItem();i.setItem(items.getReferenceById(line.itemId()));i.setAgreedQuantity(line.quantity());
            i.setUnitRate(line.unitRate());i.setRentalRate(line.rentalRate());i.setNotes(line.notes());lines.add(i);}a.replaceItems(lines);
        a.setCreatedBy(actor());a.setUpdatedBy(actor());Agreement saved=agreements.save(a);quotations.markConverted(quotationId);
        log("QUOTATION_CONVERTED_TO_AGREEMENT",saved,"From quotation "+quotationId,http);return response(saved);}
    @Transactional public AgreementResponse generate(Long id,HttpServletRequest http){
        Agreement a=detailed(id);if(a.getStatus()!=AgreementStatus.DRAFT)
            throw new BusinessRuleException("AGREEMENT_ALREADY_GENERATED","Only a draft agreement can be generated");
        Path directory=storageRoot.resolve("agreements").resolve(String.valueOf(id)).normalize();if(!directory.startsWith(storageRoot))
            throw new BusinessRuleException("INVALID_FILE_PATH","Invalid agreement path");
        String filename=a.getAgreementNumber()+".docx";Path destination=directory.resolve(filename).normalize();
        try{Files.createDirectories(directory);writeAgreementDocument(a,destination);}catch(IOException e){
            throw new BusinessRuleException("AGREEMENT_GENERATION_FAILED","Unable to generate agreement document");}
        a.setGeneratedFilename(filename);a.setGeneratedStoragePath(storageRoot.relativize(destination).toString());a.setGeneratedAt(Instant.now());
        a.setStatus(AgreementStatus.GENERATED);a.setUpdatedBy(actor());Agreement saved=agreements.save(a);
        log("AGREEMENT_GENERATED",saved,filename,http);return response(saved);}
    @Transactional public AgreementResponse activate(Long id,HttpServletRequest http){
        Agreement a=detailed(id);if(a.getStatus()!=AgreementStatus.GENERATED)
            throw new BusinessRuleException("AGREEMENT_NOT_GENERATED","Generate the agreement before activation");
        if(a.getExpiryDate()!=null&&a.getExpiryDate().isBefore(LocalDate.now()))throw new BusinessRuleException("AGREEMENT_EXPIRED","Agreement has already expired");
        a.setStatus(AgreementStatus.ACTIVE);a.setUpdatedBy(actor());Agreement saved=agreements.save(a);
        log("AGREEMENT_ACTIVATED",saved,"Agreement activated",http);return response(saved);}
    @Transactional public AgreementResponse terminate(Long id,HttpServletRequest http){
        Agreement a=detailed(id);if(a.getStatus()!=AgreementStatus.ACTIVE)
            throw new BusinessRuleException("INVALID_AGREEMENT_TRANSITION","Only active agreements can be terminated");
        a.setStatus(AgreementStatus.TERMINATED);a.setUpdatedBy(actor());Agreement saved=agreements.save(a);
        log("AGREEMENT_TERMINATED",saved,"Agreement terminated",http);return response(saved);}
    @Transactional(readOnly=true)public Download downloadGenerated(Long id){
        Agreement a=detailed(id);if(a.getGeneratedStoragePath()==null)throw new BusinessRuleException("AGREEMENT_NOT_GENERATED","Agreement has no generated document");
        Path path=storageRoot.resolve(a.getGeneratedStoragePath()).normalize();if(!path.startsWith(storageRoot)||!Files.isRegularFile(path))
            throw new BusinessRuleException("FILE_NOT_FOUND","Generated agreement file not found");
        return new Download(new FileSystemResource(path),a.getGeneratedFilename(),"application/vnd.openxmlformats-officedocument.wordprocessingml.document");}
    @Transactional public AgreementTemplateResponse uploadTemplate(String name,String description,MultipartFile file,HttpServletRequest http){
        if(name==null||name.isBlank())throw new BusinessRuleException("TEMPLATE_NAME_REQUIRED","Template name is required");
        if(templates.existsByNameIgnoreCase(name.trim()))throw new BusinessRuleException("TEMPLATE_NAME_ALREADY_EXISTS","Template name already exists");
        if(file.isEmpty())throw new BusinessRuleException("FILE_EMPTY","Template file is empty");if(file.getSize()>10L*1024*1024)
            throw new BusinessRuleException("FILE_TOO_LARGE","Template must not exceed 10 MB");
        String content=Optional.ofNullable(file.getContentType()).orElse("");if(!TEMPLATE_TYPES.contains(content))
            throw new BusinessRuleException("FILE_TYPE_NOT_ALLOWED","Agreement templates must be PDF or DOCX");
        String original=Paths.get(Optional.ofNullable(file.getOriginalFilename()).orElse("template")).getFileName().toString();
        String ext=content.equals("application/pdf")?".pdf":".docx";String stored=UUID.randomUUID()+ext;
        Path dir=storageRoot.resolve("agreement-templates").normalize();Path destination=dir.resolve(stored).normalize();
        try{Files.createDirectories(dir);file.transferTo(destination);}catch(IOException e){throw new BusinessRuleException("FILE_STORAGE_FAILED","Unable to store template");}
        AgreementTemplate t=new AgreementTemplate();t.setName(name.trim());t.setDescription(trim(description));t.setOriginalFilename(original);
        t.setStoredFilename(stored);t.setContentType(content);t.setFileSize(file.getSize());t.setStoragePath(storageRoot.relativize(destination).toString());
        t.setCreatedBy(actor());t.setUpdatedBy(actor());AgreementTemplate saved=templates.save(t);User user=users.findByUsernameIgnoreCase(actor()).orElse(null);
        audit.log(user==null?null:user.getId(),actor(),"AGREEMENT_TEMPLATE_UPLOADED","AgreementTemplate",String.valueOf(saved.getId()),original,http);
        return templateResponse(saved);}
    @Transactional(readOnly=true)public List<AgreementTemplateResponse> templates(){return templates.findAllByOrderByNameAsc().stream().map(this::templateResponse).toList();}
    @Transactional(readOnly=true)public Download downloadTemplate(Long id){AgreementTemplate t=templates.findById(id)
        .orElseThrow(()->new BusinessRuleException("TEMPLATE_NOT_FOUND","Agreement template not found"));Path path=storageRoot.resolve(t.getStoragePath()).normalize();
        if(!path.startsWith(storageRoot)||!Files.isRegularFile(path))throw new BusinessRuleException("FILE_NOT_FOUND","Template file not found");
        return new Download(new FileSystemResource(path),t.getOriginalFilename(),t.getContentType());}
    @Override @Transactional(readOnly=true)public OrderAgreement requireForOrder(Long id){Agreement a=detailed(id);
        if(a.getStatus()!=AgreementStatus.ACTIVE&&a.getStatus()!=AgreementStatus.GENERATED)
            throw new BusinessRuleException("AGREEMENT_NOT_ORDERABLE","Agreement must be generated or active");
        if(a.getExpiryDate()!=null&&a.getExpiryDate().isBefore(LocalDate.now()))throw new BusinessRuleException("AGREEMENT_EXPIRED","Agreement has expired");
        return new OrderAgreement(a.getId(),a.getParty().getId(),a.getSite().getId(),a.getStatus(),
                a.getItems().stream().map(i->new OrderAgreementItem(i.getItem().getId(),i.getAgreedQuantity())).toList());}
    private void apply(Agreement a,AgreementRequest r){Party p=parties.findById(r.partyId()).filter(Party::isActive)
        .orElseThrow(()->new BusinessRuleException("PARTY_NOT_FOUND_OR_INACTIVE","Active party not found"));Site s=sites.findById(r.siteId())
        .orElseThrow(()->new BusinessRuleException("SITE_NOT_FOUND","Site not found"));if(!s.getParty().getId().equals(p.getId()))
        throw new BusinessRuleException("SITE_PARTY_MISMATCH","Site does not belong to selected party");if(s.getStatus()==SiteStatus.CLOSED)
        throw new BusinessRuleException("SITE_CLOSED","Closed site cannot receive an agreement");if(r.expiryDate()!=null&&r.expiryDate().isBefore(r.effectiveDate()))
        throw new BusinessRuleException("INVALID_AGREEMENT_DATES","Expiry cannot precede effective date");Set<Long>unique=new HashSet<>();List<AgreementItem>lines=new ArrayList<>();
        for(AgreementItemRequest line:r.items()){if(!unique.add(line.itemId()))throw new BusinessRuleException("DUPLICATE_ITEM_LINE","Each item may appear only once");
            Item item=items.findById(line.itemId()).filter(Item::isActive).orElseThrow(()->new BusinessRuleException("ITEM_NOT_FOUND_OR_INACTIVE","Active item not found"));
            AgreementItem ai=new AgreementItem();ai.setItem(item);ai.setAgreedQuantity(line.agreedQuantity());ai.setUnitRate(line.unitRate());
            ai.setRentalRate(line.rentalRate());ai.setNotes(trim(line.notes()));lines.add(ai);}a.setParty(p);a.setSite(s);a.setTemplate(template(r.templateId()));
        a.setEffectiveDate(r.effectiveDate());a.setExpiryDate(r.expiryDate());a.setRentalType(r.rentalType());a.setSecurityDeposit(r.securityDeposit());
        a.setTransportCharge(r.transportCharge());a.setLoadingCharge(r.loadingCharge());a.setUnloadingCharge(r.unloadingCharge());
        a.setTerms(trim(r.terms()));a.setNotes(trim(r.notes()));a.replaceItems(lines);}
    private AgreementTemplate template(Long id){if(id==null)return null;return templates.findById(id).filter(AgreementTemplate::isActive)
        .orElseThrow(()->new BusinessRuleException("TEMPLATE_NOT_FOUND_OR_INACTIVE","Active agreement template not found"));}
    private void writeAgreementDocument(Agreement a,Path destination)throws IOException{
        XWPFDocument templateDocument=null;if(a.getTemplate()!=null&&a.getTemplate().getContentType().contains("wordprocessingml")){
            Path path=storageRoot.resolve(a.getTemplate().getStoragePath()).normalize();if(path.startsWith(storageRoot)&&Files.isRegularFile(path))
                try(InputStream in=Files.newInputStream(path)){templateDocument=new XWPFDocument(in);}}
        final XWPFDocument doc=templateDocument==null?new XWPFDocument():templateDocument;try(doc){
            Map<String,String>values=Map.of("{{agreementNumber}}",a.getAgreementNumber(),"{{partyName}}",a.getParty().getLegalName(),
                "{{siteName}}",a.getSite().getSiteName(),"{{agreementDate}}",a.getEffectiveDate().toString(),
                "{{securityDeposit}}",a.getSecurityDeposit().toPlainString(),"{{rentalType}}",a.getRentalType().name());
            if(doc.getParagraphs().isEmpty()){XWPFParagraph title=doc.createParagraph();title.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun run=title.createRun();run.setBold(true);run.setFontSize(18);run.setText("SHUTTERING MATERIAL AGREEMENT");
                doc.createParagraph().createRun().setText("Agreement: "+a.getAgreementNumber());
                doc.createParagraph().createRun().setText("Party: "+a.getParty().getLegalName());
                doc.createParagraph().createRun().setText("Site: "+a.getSite().getSiteName());
                doc.createParagraph().createRun().setText("Effective date: "+a.getEffectiveDate());
            }else replacePlaceholders(doc,values);
            XWPFTable table=doc.createTable(a.getItems().size()+1,6);String[]head={"Item code","Item","Quantity","Unit","Unit rate","Rental rate"};
            for(int c=0;c<head.length;c++)table.getRow(0).getCell(c).setText(head[c]);int row=1;
            for(AgreementItem i:a.getItems()){String[]v={i.getItem().getItemCode(),i.getItem().getItemName(),i.getAgreedQuantity().toPlainString(),
                i.getItem().getUnit(),i.getUnitRate().toPlainString(),i.getRentalRate().toPlainString()};for(int c=0;c<v.length;c++)table.getRow(row).getCell(c).setText(v[c]);row++;}
            if(a.getTerms()!=null)doc.createParagraph().createRun().setText("Terms: "+a.getTerms());try(OutputStream out=Files.newOutputStream(destination)){doc.write(out);}}
    }
    private void replacePlaceholders(XWPFDocument doc,Map<String,String>values){for(XWPFParagraph p:doc.getParagraphs()){String text=p.getText();
        for(var e:values.entrySet())text=text.replace(e.getKey(),e.getValue());if(!text.equals(p.getText())){for(int i=p.getRuns().size()-1;i>=0;i--)p.removeRun(i);p.createRun().setText(text);}}}
    private Agreement detailed(Long id){return agreements.findDetailedById(id).orElseThrow(()->new BusinessRuleException("AGREEMENT_NOT_FOUND","Agreement not found"));}
    private void requireDraft(Agreement a){if(a.getStatus()!=AgreementStatus.DRAFT)throw new BusinessRuleException("AGREEMENT_IMMUTABLE","Only draft agreements can be edited");}
    private AgreementResponse response(Agreement a){return new AgreementResponse(a.getId(),a.getAgreementNumber(),a.getQuotation()==null?null:a.getQuotation().getId(),
        a.getTemplate()==null?null:a.getTemplate().getId(),a.getTemplate()==null?null:a.getTemplate().getName(),a.getParty().getId(),a.getParty().getLegalName(),
        a.getSite().getId(),a.getSite().getSiteName(),a.getEffectiveDate(),a.getExpiryDate(),a.getRentalType(),a.getStatus(),a.getSecurityDeposit(),
        a.getTransportCharge(),a.getLoadingCharge(),a.getUnloadingCharge(),a.getTerms(),a.getNotes(),a.getGeneratedStoragePath()!=null,a.getGeneratedFilename(),
        a.getGeneratedAt(),a.getItems().stream().map(i->new AgreementItemResponse(i.getId(),i.getItem().getId(),i.getItem().getItemCode(),i.getItem().getItemName(),
            i.getItem().getUnit(),i.getAgreedQuantity(),i.getUnitRate(),i.getRentalRate(),i.getNotes())).toList(),a.getVersion(),a.getCreatedAt(),a.getUpdatedAt());}
    private AgreementTemplateResponse templateResponse(AgreementTemplate t){return new AgreementTemplateResponse(t.getId(),t.getName(),t.getDescription(),
        t.getOriginalFilename(),t.getContentType(),t.getFileSize(),t.isActive(),t.getVersion(),t.getCreatedAt());}
    private String number(){return "AGR-"+LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)+"-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);}
    private String actor(){var a=SecurityContextHolder.getContext().getAuthentication();return a==null?"system":a.getName();}
    private String trim(String v){return v==null||v.isBlank()?null:v.trim();}private String joinNotes(String a,String b){if(a==null)return trim(b);if(b==null)return trim(a);return a.trim()+"\n"+b.trim();}
    private void log(String action,Agreement a,String description,HttpServletRequest req){User user=users.findByUsernameIgnoreCase(actor()).orElse(null);
        audit.log(user==null?null:user.getId(),actor(),action,"Agreement",String.valueOf(a.getId()),description,req);}
    public record Download(Resource resource,String filename,String contentType){}
}
