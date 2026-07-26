package com.stocksync.quotation.service;

import com.stocksync.audit.service.UserActivityLogService;
import com.stocksync.auth.repository.UserRepository;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.common.numbering.*;
import com.stocksync.inventory.entity.Item;
import com.stocksync.inventory.repository.ItemRepository;
import com.stocksync.party.entity.Party;
import com.stocksync.party.repository.PartyRepository;
import com.stocksync.quotation.dto.*;
import com.stocksync.quotation.entity.*;
import com.stocksync.quotation.repository.QuotationRepository;
import com.stocksync.site.entity.*;
import com.stocksync.site.repository.SiteRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuotationService implements QuotationAccess {
    private final QuotationRepository quotations; private final PartyRepository parties; private final SiteRepository sites;
    private final ItemRepository items; private final QuotationTemplateService templates; private final DocumentNumberService numbers;
    private final QuotationCalculationService calculations; private final UserRepository users; private final UserActivityLogService audit;
    public QuotationService(QuotationRepository quotations,PartyRepository parties,SiteRepository sites,ItemRepository items,
            QuotationTemplateService templates,DocumentNumberService numbers,QuotationCalculationService calculations,
            UserRepository users,UserActivityLogService audit){
        this.quotations=quotations;this.parties=parties;this.sites=sites;this.items=items;this.templates=templates;
        this.numbers=numbers;this.calculations=calculations;this.users=users;this.audit=audit;
    }

    @Transactional(readOnly=true)
    public Page<QuotationResponse> list(String search,QuotationStatus status,Long partyId,Long siteId,
            LocalDate quotationDateFrom,LocalDate quotationDateTo,LocalDate validUntilFrom,LocalDate validUntilTo,Pageable pageable){
        return quotations.findAll((root,q,cb)->{List<Predicate> p=new ArrayList<>();
            if(search!=null&&!search.isBlank()){String t="%"+search.trim().toLowerCase(Locale.ROOT)+"%";
                p.add(cb.or(cb.like(cb.lower(root.get("quotationNumber")),t),cb.like(cb.lower(root.get("party").get("legalName")),t),
                        cb.like(cb.lower(root.get("site").get("siteName")),t)));}
            if(status!=null)p.add(cb.equal(root.get("status"),status));if(partyId!=null)p.add(cb.equal(root.get("party").get("id"),partyId));
            if(siteId!=null)p.add(cb.equal(root.get("site").get("id"),siteId));
            if(quotationDateFrom!=null)p.add(cb.greaterThanOrEqualTo(root.get("quotationDate"),quotationDateFrom));
            if(quotationDateTo!=null)p.add(cb.lessThanOrEqualTo(root.get("quotationDate"),quotationDateTo));
            if(validUntilFrom!=null)p.add(cb.greaterThanOrEqualTo(root.get("validUntil"),validUntilFrom));
            if(validUntilTo!=null)p.add(cb.lessThanOrEqualTo(root.get("validUntil"),validUntilTo));
            return cb.and(p.toArray(Predicate[]::new));},pageable).map(this::response);
    }
    @Transactional(readOnly=true) public QuotationResponse get(Long id){return response(detailed(id));}
    @Transactional public void pdfGenerated(Long id,HttpServletRequest req){Quotation q=detailed(id);log("QUOTATION_PDF_GENERATED",q,"PDF generated",req);}
    @Transactional public QuotationResponse create(QuotationRequest r,HttpServletRequest req){
        Quotation q=new Quotation();q.setQuotationNumber(numbers.next(DocumentType.QUOTATION,r.quotationDate()));
        q.setStatus(QuotationStatus.DRAFT);apply(q,r,true);q.setCreatedBy(actor());q.setUpdatedBy(actor());
        q=quotations.save(q);log("QUOTATION_CREATED",q,"Draft created",req);return response(q);
    }
    @Transactional public QuotationResponse update(Long id,QuotationRequest r,HttpServletRequest req){
        Quotation q=detailed(id);requireDraft(q);checkVersion(q,r.version());
        q.getItems().clear();quotations.flush();apply(q,r,false);q.setUpdatedBy(actor());q=quotations.save(q);
        log("QUOTATION_UPDATED",q,"Draft updated",req);return response(q);
    }
    @Transactional public QuotationResponse cloneQuotation(Long id,HttpServletRequest req){
        Quotation s=detailed(id),q=new Quotation();q.setQuotationNumber(numbers.next(DocumentType.QUOTATION,LocalDate.now()));
        q.setStatus(QuotationStatus.DRAFT);q.setQuotationTemplate(s.getQuotationTemplate());q.setParty(s.getParty());q.setPartyNameSnapshot(s.getPartyNameSnapshot());q.setSite(s.getSite());q.setSiteNameSnapshot(s.getSiteNameSnapshot());
        q.setQuotationDate(LocalDate.now());q.setValidUntil(LocalDate.now().plusDays(Math.max(1,java.time.temporal.ChronoUnit.DAYS.between(s.getQuotationDate(),s.getValidUntil()))));
        q.setRentalType(s.getRentalType());copyCommercial(s,q);q.replaceItems(s.getItems().stream().map(this::copyItem).toList());
        calculations.calculate(q);q.setCreatedBy(actor());q.setUpdatedBy(actor());q=quotations.save(q);
        log("QUOTATION_CLONED",q,"Cloned from "+s.getQuotationNumber(),req);return response(q);
    }
    @Transactional public QuotationResponse transition(Long id,QuotationStatus target,String reason,HttpServletRequest req){
        Quotation q=detailed(id);QuotationStatus from=q.getStatus();Instant now=Instant.now();String who=actor();
        boolean valid=(from==QuotationStatus.DRAFT&&target==QuotationStatus.SENT)
                ||(from==QuotationStatus.SENT&&(target==QuotationStatus.APPROVED||target==QuotationStatus.REJECTED))
                ||((from==QuotationStatus.DRAFT||from==QuotationStatus.SENT)&&target==QuotationStatus.CANCELLED);
        if(!valid)throw new BusinessRuleException("INVALID_QUOTATION_TRANSITION","Cannot change quotation from "+from+" to "+target);
        if(target==QuotationStatus.SENT&&q.getItems().isEmpty())throw new BusinessRuleException("QUOTATION_ITEMS_REQUIRED","At least one item is required before sending");
        if((target==QuotationStatus.SENT||target==QuotationStatus.APPROVED)&&q.getValidUntil().isBefore(LocalDate.now()))
            throw new BusinessRuleException("QUOTATION_EXPIRED","Quotation validity has expired");
        if((target==QuotationStatus.REJECTED||target==QuotationStatus.CANCELLED)&&(reason==null||reason.isBlank()))
            throw new BusinessRuleException("REASON_REQUIRED","A reason is required");
        q.setStatus(target);q.setUpdatedBy(who);
        if(target==QuotationStatus.SENT){q.setSentAt(now);q.setSentBy(who);}
        if(target==QuotationStatus.APPROVED){q.setApprovedAt(now);q.setApprovedBy(who);}
        if(target==QuotationStatus.REJECTED){q.setRejectedAt(now);q.setRejectedBy(who);q.setRejectionReason(reason.trim());}
        if(target==QuotationStatus.CANCELLED){q.setCancelledAt(now);q.setCancelledBy(who);q.setCancellationReason(reason.trim());}
        q=quotations.save(q);log("QUOTATION_"+target,q,from+" to "+target,req);return response(q);
    }

    @Override @Transactional(readOnly=true)
    public ApprovedQuotation requireApprovedForConversion(Long id){
        Quotation q=detailed(id);if(q.getStatus()!=QuotationStatus.APPROVED)throw new BusinessRuleException("QUOTATION_NOT_APPROVED","Only an approved quotation can become an agreement");
        return new ApprovedQuotation(q.getId(),q.getParty().getId(),q.getSite().getId(),q.getQuotationDate(),q.getValidUntil(),q.getRentalType(),
                q.getTransportCharge(),q.getLoadingCharge(),q.getUnloadingCharge(),q.getTerms(),q.getNotes(),
                q.getItems().stream().map(i->new ApprovedItem(i.getItem().getId(),i.getQuantity(),i.getUnitRate(),i.getRentalRate(),i.getDescriptionSnapshot())).toList());
    }
    @Override @Transactional public void markConverted(Long id){Quotation q=detailed(id);if(q.getStatus()!=QuotationStatus.APPROVED)throw new BusinessRuleException("QUOTATION_NOT_APPROVED","Quotation is no longer approved");q.setStatus(QuotationStatus.CONVERTED);q.setUpdatedBy(actor());}

    private void apply(Quotation q,QuotationRequest r,boolean creating){
        Party party=parties.findById(r.partyId()).filter(Party::isActive).orElseThrow(()->new BusinessRuleException("PARTY_NOT_FOUND_OR_INACTIVE","Active party not found"));
        Site site=sites.findById(r.siteId()).orElseThrow(()->new BusinessRuleException("SITE_NOT_FOUND","Site not found"));
        if(!site.getParty().getId().equals(party.getId()))throw new BusinessRuleException("SITE_PARTY_MISMATCH","Site does not belong to selected party");
        if(site.getStatus()==SiteStatus.CLOSED)throw new BusinessRuleException("SITE_CLOSED","Closed site cannot receive a quotation");
        if(r.validUntil().isBefore(r.quotationDate()))throw new BusinessRuleException("INVALID_QUOTATION_DATES","Valid-until cannot precede quotation date");
        q.setQuotationTemplate(creating?templates.requireActive(r.quotationTemplateId()):templateForUpdate(q,r.quotationTemplateId()));
        q.setParty(party);q.setPartyNameSnapshot(party.getLegalName());q.setSite(site);q.setSiteNameSnapshot(site.getSiteName());q.setQuotationDate(r.quotationDate());q.setValidUntil(r.validUntil());q.setRentalType(r.rentalType());
        q.setDiscountType(r.discountType());q.setDiscountValue(r.discountValue());q.setCgstRate(r.cgstRate());q.setSgstRate(r.sgstRate());q.setIgstRate(r.igstRate());
        q.setTransportCharge(r.transportCharge());q.setLoadingCharge(r.loadingCharge());q.setUnloadingCharge(r.unloadingCharge());q.setOtherCharge(r.otherCharge());
        q.setRoundOff(r.roundOff());q.setSecurityDeposit(r.securityDeposit());
        q.setTerms(creating&&isBlank(r.terms())?trim(q.getQuotationTemplate().getDefaultTerms()):trim(r.terms()));
        q.setNotes(creating&&isBlank(r.notes())?trim(q.getQuotationTemplate().getDefaultNotes()):trim(r.notes()));
        Set<Long> unique=new HashSet<>();List<QuotationItem> lines=new ArrayList<>();int sequence=1;
        for(QuotationItemRequest line:r.items()){if(!unique.add(line.itemId()))throw new BusinessRuleException("DUPLICATE_ITEM_LINE","Each item may appear only once");
            Item item=items.findById(line.itemId()).filter(Item::isActive).orElseThrow(()->new BusinessRuleException("ITEM_NOT_FOUND_OR_INACTIVE","Active item not found"));
            QuotationItem qi=new QuotationItem();qi.setItem(item);qi.setItemCodeSnapshot(item.getItemCode());qi.setItemNameSnapshot(item.getItemName());
            qi.setDescriptionSnapshot(trim(line.description()));qi.setSizeSnapshot(item.getSize());qi.setUnitSnapshot(item.getUnit());
            qi.setQuantity(line.quantity());qi.setUnitRate(line.rate());qi.setRentalRate(line.rate());qi.setRentalType(line.rentalType());
            qi.setArea(line.area());qi.setWeight(line.weight());qi.setLineAmount(line.quantity().multiply(line.rate()).setScale(2,RoundingMode.HALF_UP));qi.setSequence(sequence++);lines.add(qi);}
        q.replaceItems(lines);calculations.calculate(q);
    }
    private QuotationTemplate templateForUpdate(Quotation q,Long id){if(q.getQuotationTemplate()!=null&&q.getQuotationTemplate().getId().equals(id))return q.getQuotationTemplate();return templates.requireActive(id);}
    private void copyCommercial(Quotation s,Quotation q){q.setDiscountType(s.getDiscountType());q.setDiscountValue(s.getDiscountValue());q.setCgstRate(s.getCgstRate());q.setSgstRate(s.getSgstRate());q.setIgstRate(s.getIgstRate());q.setTransportCharge(s.getTransportCharge());q.setLoadingCharge(s.getLoadingCharge());q.setUnloadingCharge(s.getUnloadingCharge());q.setOtherCharge(s.getOtherCharge());q.setRoundOff(s.getRoundOff());q.setSecurityDeposit(s.getSecurityDeposit());q.setTerms(s.getTerms());q.setNotes(s.getNotes());}
    private QuotationItem copyItem(QuotationItem s){QuotationItem i=new QuotationItem();i.setItem(s.getItem());i.setItemCodeSnapshot(s.getItemCodeSnapshot());i.setItemNameSnapshot(s.getItemNameSnapshot());i.setDescriptionSnapshot(s.getDescriptionSnapshot());i.setSizeSnapshot(s.getSizeSnapshot());i.setUnitSnapshot(s.getUnitSnapshot());i.setQuantity(s.getQuantity());i.setUnitRate(s.getUnitRate());i.setRentalRate(s.getRentalRate());i.setRentalType(s.getRentalType());i.setArea(s.getArea());i.setWeight(s.getWeight());i.setLineAmount(s.getLineAmount());i.setSequence(s.getSequence());return i;}
    private void requireDraft(Quotation q){if(q.getStatus()!=QuotationStatus.DRAFT)throw new BusinessRuleException("QUOTATION_IMMUTABLE","Only draft quotations can be edited");}
    private void checkVersion(Quotation q,Long version){if(version==null||q.getVersion()!=version)throw new ObjectOptimisticLockingFailureException(Quotation.class,q.getId());}
    private Quotation detailed(Long id){return quotations.findDetailedById(id).orElseThrow(()->new BusinessRuleException("QUOTATION_NOT_FOUND","Quotation not found"));}
    private QuotationResponse response(Quotation q){QuotationTemplate t=q.getQuotationTemplate();return new QuotationResponse(q.getId(),q.getQuotationNumber(),t==null?null:t.getId(),t==null?null:t.getName(),t==null?null:t.getCompanyName(),t==null?null:t.getCompanyAddress(),t==null?null:t.getCompanyGstin(),t==null?null:t.getHeaderText(),t==null?null:t.getFooterText(),q.getParty().getId(),q.getPartyNameSnapshot(),q.getSite().getId(),q.getSiteNameSnapshot(),q.getQuotationDate(),q.getValidUntil(),q.getRentalType(),q.getStatus(),q.getSubtotal(),q.getDiscountType(),q.getDiscountValue(),q.getDiscountAmount(),q.getTaxableAmount(),q.getCgstRate(),q.getCgstAmount(),q.getSgstRate(),q.getSgstAmount(),q.getIgstRate(),q.getIgstAmount(),q.getTotalTax(),q.getTransportCharge(),q.getLoadingCharge(),q.getUnloadingCharge(),q.getOtherCharge(),q.getRoundOff(),q.getGrandTotal(),q.getSecurityDeposit(),q.getTerms(),q.getNotes(),q.getRejectionReason(),q.getSentAt(),q.getSentBy(),q.getApprovedAt(),q.getApprovedBy(),q.getRejectedAt(),q.getRejectedBy(),q.getCancelledAt(),q.getCancelledBy(),q.getCancellationReason(),q.getItems().stream().map(i->new QuotationItemResponse(i.getId(),i.getItem().getId(),i.getItemCodeSnapshot(),i.getItemNameSnapshot(),i.getDescriptionSnapshot(),i.getSizeSnapshot(),i.getUnitSnapshot(),i.getQuantity(),i.getRentalRate(),i.getRentalType(),i.getArea(),i.getWeight(),i.getLineAmount(),i.getSequence(),i.getVersion())).toList(),q.getVersion(),q.getCreatedAt(),q.getCreatedBy(),q.getUpdatedAt(),q.getUpdatedBy());}
    private String actor(){var a=SecurityContextHolder.getContext().getAuthentication();return a==null?"system":a.getName();}
    private String trim(String v){return v==null||v.isBlank()?null:v.trim();}
    private boolean isBlank(String v){return v==null||v.isBlank();}
    private void log(String action,Quotation q,String description,HttpServletRequest req){var u=users.findByUsernameIgnoreCase(actor()).orElse(null);audit.log(u==null?null:u.getId(),actor(),action,"Quotation",String.valueOf(q.getId()),description,req);}
}
