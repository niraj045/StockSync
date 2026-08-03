package com.stocksync.site.service;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.party.entity.Party;
import com.stocksync.party.repository.PartyRepository;
import com.stocksync.site.dto.*;
import com.stocksync.site.entity.*;
import com.stocksync.site.repository.SiteRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

@Service
public class SiteService {
    private final SiteRepository sites; private final PartyRepository parties;
    public SiteService(SiteRepository sites,PartyRepository parties){this.sites=sites;this.parties=parties;}
    @Transactional(readOnly=true)
    public Page<SiteResponse> list(String search,Long partyId,SiteStatus status,Boolean defaulter,Pageable pageable){
        return sites.findAll((root,q,cb)->{List<Predicate> p=new ArrayList<>();
            if(search!=null&&!search.isBlank()){String t="%"+search.trim().toLowerCase(Locale.ROOT)+"%";
                p.add(cb.or(cb.like(cb.lower(root.get("siteName")),t),cb.like(cb.lower(root.get("siteCode")),t)));}
            if(partyId!=null)p.add(cb.equal(root.get("party").get("id"),partyId));
            if(status!=null)p.add(cb.equal(root.get("status"),status));
            if(defaulter!=null)p.add(cb.equal(root.get("defaulter"),defaulter));
            return cb.and(p.toArray(Predicate[]::new));},pageable).map(this::response);
    }
    @Transactional(readOnly=true) public SiteResponse get(Long id){return response(entity(id));}
    @Transactional public SiteResponse create(SiteRequest r){validateCode(r.siteCode(),null);Site s=new Site();apply(s,r);
        s.setCreatedBy(auditor());s.setUpdatedBy(auditor());return response(sites.save(s));}
    @Transactional public SiteResponse update(Long id,SiteRequest r){Site s=entity(id);
        if(r.version()==null||s.getVersion()!=r.version())throw new ObjectOptimisticLockingFailureException(Site.class,id);
        validateCode(r.siteCode(),id);apply(s,r);s.setUpdatedBy(auditor());return response(sites.save(s));}
    private void apply(Site s,SiteRequest r){Party p=parties.findById(r.partyId()).orElseThrow(()->new BusinessRuleException("PARTY_NOT_FOUND","Party not found"));
        if(!p.isActive())throw new BusinessRuleException("PARTY_INACTIVE","Site party is inactive");
        if(r.startDate()!=null&&r.expectedEndDate()!=null&&r.expectedEndDate().isBefore(r.startDate()))
            throw new BusinessRuleException("INVALID_SITE_DATES","Expected end date cannot be before start date");
        if(r.status()==SiteStatus.CLOSED&&r.closedDate()==null)throw new BusinessRuleException("CLOSED_DATE_REQUIRED","Closed date is required for a closed site");
        if(r.closedDate()!=null&&r.startDate()!=null&&r.closedDate().isBefore(r.startDate()))
            throw new BusinessRuleException("INVALID_SITE_DATES","Closed date cannot be before start date");
        s.setParty(p);s.setSiteName(r.siteName().trim());s.setSiteCode(r.siteCode().trim().toUpperCase(Locale.ROOT));
        s.setAddress(trim(r.address()));s.setContactPerson(trim(r.contactPerson()));s.setStartDate(r.startDate());
        s.setExpectedEndDate(r.expectedEndDate());s.setStatus(r.status());
        s.setDefaulter(r.defaulter()||r.status()==SiteStatus.DEFAULTER);
        s.setClosedDate(r.status()==SiteStatus.CLOSED?r.closedDate():null);s.setNotes(trim(r.notes()));s.setExcelTemplateCode(trim(r.excelTemplateCode()));}
    private void validateCode(String code,Long id){boolean exists=id==null?sites.existsBySiteCodeIgnoreCase(code.trim()):sites.existsBySiteCodeIgnoreCaseAndIdNot(code.trim(),id);
        if(exists)throw new BusinessRuleException("SITE_CODE_ALREADY_EXISTS","Site code already exists");}
    private Site entity(Long id){return sites.findById(id).orElseThrow(()->new BusinessRuleException("SITE_NOT_FOUND","Site not found"));}
    private SiteResponse response(Site s){return new SiteResponse(s.getId(),s.getParty().getId(),s.getParty().getLegalName(),s.getSiteName(),s.getSiteCode(),
        s.getAddress(),s.getContactPerson(),s.getStartDate(),s.getExpectedEndDate(),s.getStatus(),s.isDefaulter(),s.getClosedDate(),s.getNotes(),s.getExcelTemplateCode(),s.getVersion());}
    private String auditor(){var a=SecurityContextHolder.getContext().getAuthentication();return a==null?"system":a.getName();}
    private String trim(String v){return v==null||v.isBlank()?null:v.trim();}
}
