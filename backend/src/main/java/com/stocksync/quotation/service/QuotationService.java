package com.stocksync.quotation.service;

import com.stocksync.audit.service.UserActivityLogService;
import com.stocksync.auth.entity.User;
import com.stocksync.auth.repository.UserRepository;
import com.stocksync.common.exception.BusinessRuleException;
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
import org.springframework.data.domain.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class QuotationService implements QuotationAccess {
    private final QuotationRepository quotations; private final PartyRepository parties; private final SiteRepository sites;
    private final ItemRepository items; private final UserRepository users; private final UserActivityLogService audit;
    public QuotationService(QuotationRepository quotations,PartyRepository parties,SiteRepository sites,ItemRepository items,
            UserRepository users,UserActivityLogService audit){this.quotations=quotations;this.parties=parties;this.sites=sites;
        this.items=items;this.users=users;this.audit=audit;}

    @Transactional(readOnly=true)
    public Page<QuotationResponse> list(String search,QuotationStatus status,Long partyId,Long siteId,Pageable pageable){
        return quotations.findAll((root,q,cb)->{List<Predicate> p=new ArrayList<>();
            if(search!=null&&!search.isBlank()){String t="%"+search.trim().toLowerCase(Locale.ROOT)+"%";
                p.add(cb.or(cb.like(cb.lower(root.get("quotationNumber")),t),cb.like(cb.lower(root.get("party").get("legalName")),t),
                        cb.like(cb.lower(root.get("site").get("siteName")),t)));}
            if(status!=null)p.add(cb.equal(root.get("status"),status));
            if(partyId!=null)p.add(cb.equal(root.get("party").get("id"),partyId));
            if(siteId!=null)p.add(cb.equal(root.get("site").get("id"),siteId));
            return cb.and(p.toArray(Predicate[]::new));},pageable).map(this::response);
    }
    @Transactional(readOnly=true) public QuotationResponse get(Long id){return response(detailed(id));}
    @Transactional public QuotationResponse create(QuotationRequest r,HttpServletRequest http){
        Quotation q=new Quotation();q.setQuotationNumber(number());q.setStatus(QuotationStatus.DRAFT);apply(q,r);
        q.setCreatedBy(actor());q.setUpdatedBy(actor());Quotation saved=quotations.save(q);
        log("QUOTATION_CREATED",saved,"Draft created",http);return response(saved);
    }
    @Transactional public QuotationResponse update(Long id,QuotationRequest r,HttpServletRequest http){
        Quotation q=detailed(id);requireDraft(q);
        if(r.version()==null||q.getVersion()!=r.version())throw new ObjectOptimisticLockingFailureException(Quotation.class,id);
        q.getItems().clear();quotations.flush();
        apply(q,r);q.setUpdatedBy(actor());Quotation saved=quotations.save(q);
        log("QUOTATION_UPDATED",saved,"Draft updated",http);return response(saved);
    }
    @Transactional public QuotationResponse cloneQuotation(Long id,HttpServletRequest http){
        Quotation source=detailed(id);Quotation clone=new Quotation();clone.setQuotationNumber(number());clone.setStatus(QuotationStatus.DRAFT);
        clone.setParty(source.getParty());clone.setSite(source.getSite());clone.setQuotationDate(LocalDate.now());
        clone.setValidUntil(LocalDate.now().plusDays(Math.max(1,java.time.temporal.ChronoUnit.DAYS.between(source.getQuotationDate(),source.getValidUntil()))));
        clone.setRentalType(source.getRentalType());clone.setTransportCharge(source.getTransportCharge());clone.setLoadingCharge(source.getLoadingCharge());
        clone.setUnloadingCharge(source.getUnloadingCharge());clone.setTaxRate(source.getTaxRate());clone.setTerms(source.getTerms());clone.setNotes(source.getNotes());
        clone.replaceItems(source.getItems().stream().map(this::copyItem).toList());calculate(clone);clone.setCreatedBy(actor());clone.setUpdatedBy(actor());
        Quotation saved=quotations.save(clone);log("QUOTATION_CLONED",saved,"Cloned from "+source.getQuotationNumber(),http);return response(saved);
    }
    @Transactional public QuotationResponse transition(Long id,QuotationStatus target,HttpServletRequest http){
        Quotation q=detailed(id);QuotationStatus from=q.getStatus();
        boolean valid=(from==QuotationStatus.DRAFT&&target==QuotationStatus.SENT)
                ||(from==QuotationStatus.SENT&&(target==QuotationStatus.APPROVED||target==QuotationStatus.REJECTED))
                ||((from==QuotationStatus.DRAFT||from==QuotationStatus.SENT)&&target==QuotationStatus.EXPIRED);
        if(!valid)throw new BusinessRuleException("INVALID_QUOTATION_TRANSITION","Cannot change quotation from "+from+" to "+target);
        if((target==QuotationStatus.SENT||target==QuotationStatus.APPROVED)&&q.getValidUntil().isBefore(LocalDate.now()))
            throw new BusinessRuleException("QUOTATION_EXPIRED","Quotation validity has expired");
        q.setStatus(target);q.setUpdatedBy(actor());Quotation saved=quotations.save(q);
        log("QUOTATION_"+target,saved,from+" to "+target,http);return response(saved);
    }
    @Override @Transactional(readOnly=true)
    public ApprovedQuotation requireApprovedForConversion(Long id){
        Quotation q=detailed(id);if(q.getStatus()!=QuotationStatus.APPROVED)
            throw new BusinessRuleException("QUOTATION_NOT_APPROVED","Only an approved quotation can become an agreement");
        return new ApprovedQuotation(q.getId(),q.getParty().getId(),q.getSite().getId(),q.getQuotationDate(),q.getValidUntil(),
                q.getRentalType(),q.getTransportCharge(),q.getLoadingCharge(),q.getUnloadingCharge(),q.getTerms(),q.getNotes(),
                q.getItems().stream().map(i->new ApprovedItem(i.getItem().getId(),i.getQuantity(),i.getUnitRate(),i.getRentalRate(),i.getNotes())).toList());
    }
    @Override @Transactional public void markConverted(Long id){
        Quotation q=detailed(id);if(q.getStatus()!=QuotationStatus.APPROVED)
            throw new BusinessRuleException("QUOTATION_NOT_APPROVED","Quotation is no longer approved");
        q.setStatus(QuotationStatus.CONVERTED);q.setUpdatedBy(actor());
    }
    private void apply(Quotation q,QuotationRequest r){
        Party party=parties.findById(r.partyId()).filter(Party::isActive).orElseThrow(()->new BusinessRuleException("PARTY_NOT_FOUND_OR_INACTIVE","Active party not found"));
        Site site=sites.findById(r.siteId()).orElseThrow(()->new BusinessRuleException("SITE_NOT_FOUND","Site not found"));
        if(!site.getParty().getId().equals(party.getId()))throw new BusinessRuleException("SITE_PARTY_MISMATCH","Site does not belong to the selected party");
        if(site.getStatus()==SiteStatus.CLOSED)throw new BusinessRuleException("SITE_CLOSED","Closed site cannot receive a quotation");
        if(r.validUntil().isBefore(r.quotationDate()))throw new BusinessRuleException("INVALID_QUOTATION_DATES","Valid-until date cannot be before quotation date");
        Set<Long> unique=new HashSet<>();List<QuotationItem> lines=new ArrayList<>();
        for(QuotationItemRequest line:r.items()){if(!unique.add(line.itemId()))throw new BusinessRuleException("DUPLICATE_ITEM_LINE","Each item may appear only once");
            Item item=items.findById(line.itemId()).filter(Item::isActive).orElseThrow(()->new BusinessRuleException("ITEM_NOT_FOUND_OR_INACTIVE","Active item not found"));
            QuotationItem qi=new QuotationItem();qi.setItem(item);qi.setQuantity(line.quantity());qi.setUnitRate(line.unitRate());qi.setRentalRate(line.rentalRate());
            qi.setLineAmount(line.quantity().multiply(line.unitRate()).setScale(2,RoundingMode.HALF_UP));qi.setNotes(trim(line.notes()));lines.add(qi);}
        q.setParty(party);q.setSite(site);q.setQuotationDate(r.quotationDate());q.setValidUntil(r.validUntil());q.setRentalType(r.rentalType());
        q.setTransportCharge(r.transportCharge());q.setLoadingCharge(r.loadingCharge());q.setUnloadingCharge(r.unloadingCharge());
        q.setTaxRate(r.taxRate());q.setTerms(trim(r.terms()));q.setNotes(trim(r.notes()));q.replaceItems(lines);calculate(q);
    }
    private void calculate(Quotation q){BigDecimal subtotal=q.getItems().stream().map(QuotationItem::getLineAmount).reduce(BigDecimal.ZERO,BigDecimal::add)
        .add(q.getTransportCharge()).add(q.getLoadingCharge()).add(q.getUnloadingCharge()).setScale(2,RoundingMode.HALF_UP);
        BigDecimal tax=subtotal.multiply(q.getTaxRate()).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP);
        q.setSubtotal(subtotal);q.setTaxAmount(tax);q.setGrandTotal(subtotal.add(tax));}
    private QuotationItem copyItem(QuotationItem s){QuotationItem i=new QuotationItem();i.setItem(s.getItem());i.setQuantity(s.getQuantity());
        i.setUnitRate(s.getUnitRate());i.setRentalRate(s.getRentalRate());i.setLineAmount(s.getLineAmount());i.setNotes(s.getNotes());return i;}
    private void requireDraft(Quotation q){if(q.getStatus()!=QuotationStatus.DRAFT)throw new BusinessRuleException("QUOTATION_IMMUTABLE","Only draft quotations can be edited");}
    private Quotation detailed(Long id){return quotations.findDetailedById(id).orElseThrow(()->new BusinessRuleException("QUOTATION_NOT_FOUND","Quotation not found"));}
    private QuotationResponse response(Quotation q){return new QuotationResponse(q.getId(),q.getQuotationNumber(),q.getParty().getId(),q.getParty().getLegalName(),
        q.getSite().getId(),q.getSite().getSiteName(),q.getQuotationDate(),q.getValidUntil(),q.getRentalType(),q.getStatus(),q.getTransportCharge(),
        q.getLoadingCharge(),q.getUnloadingCharge(),q.getTaxRate(),q.getSubtotal(),q.getTaxAmount(),q.getGrandTotal(),q.getTerms(),q.getNotes(),
        q.getItems().stream().map(i->new QuotationItemResponse(i.getId(),i.getItem().getId(),i.getItem().getItemCode(),i.getItem().getItemName(),
            i.getItem().getUnit(),i.getQuantity(),i.getUnitRate(),i.getRentalRate(),i.getLineAmount(),i.getNotes())).toList(),
        q.getVersion(),q.getCreatedAt(),q.getUpdatedAt());}
    private String number(){return "QUO-"+LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)+"-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);}
    private String actor(){var a=SecurityContextHolder.getContext().getAuthentication();return a==null?"system":a.getName();}
    private String trim(String v){return v==null||v.isBlank()?null:v.trim();}
    private void log(String action,Quotation q,String description,HttpServletRequest req){User user=users.findByUsernameIgnoreCase(actor()).orElse(null);
        audit.log(user==null?null:user.getId(),actor(),action,"Quotation",String.valueOf(q.getId()),description,req);}
}
