package com.stocksync.party.service;

import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.party.dto.*;
import com.stocksync.party.entity.*;
import com.stocksync.party.repository.*;
import com.stocksync.site.entity.SiteStatus;
import com.stocksync.site.repository.SiteRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class PartyService {
    private final PartyRepository parties;
    private final VendorRepository vendors;
    private final SiteRepository sites;
    public PartyService(PartyRepository parties, VendorRepository vendors, SiteRepository sites) {
        this.parties=parties; this.vendors=vendors; this.sites=sites;
    }

    @Transactional(readOnly=true)
    public Page<PartyResponse> parties(String search, Boolean active, Pageable pageable) {
        return parties.findAll((root,q,cb)->{
            List<Predicate> p=new ArrayList<>();
            if(search!=null&&!search.isBlank()){
                String t="%"+search.trim().toLowerCase(Locale.ROOT)+"%";
                p.add(cb.or(cb.like(cb.lower(root.get("legalName")),t),cb.like(cb.lower(root.get("tradeName")),t),
                        cb.like(cb.lower(root.get("gstin")),t),cb.like(cb.lower(root.get("phone")),t)));
            }
            if(active!=null)p.add(cb.equal(root.get("active"),active));
            return cb.and(p.toArray(Predicate[]::new));
        },pageable).map(this::response);
    }
    @Transactional(readOnly=true) public PartyResponse party(Long id){return response(entity(id));}
    @Transactional public PartyResponse create(PartyRequest r){
        validatePartyUnique(r,null); Party p=new Party(); apply(p,r); auditCreate(p); return response(parties.save(p));
    }
    @Transactional public PartyResponse update(Long id,PartyRequest r){
        Party p=entity(id); check(p.getVersion(),r.version(),Party.class,id); validatePartyUnique(r,id);
        if(!r.active()&&sites.existsByPartyIdAndStatusNot(id, SiteStatus.CLOSED))
            throw new BusinessRuleException("PARTY_HAS_OPEN_SITES","Party with open sites cannot be deactivated");
        apply(p,r); p.setUpdatedBy(auditor()); return response(parties.save(p));
    }

    @Transactional(readOnly=true)
    public Page<VendorResponse> vendors(String search,Boolean active,Pageable pageable){
        return vendors.findAll((root,q,cb)->{
            List<Predicate> p=new ArrayList<>();
            if(search!=null&&!search.isBlank()){
                String t="%"+search.trim().toLowerCase(Locale.ROOT)+"%";
                p.add(cb.or(cb.like(cb.lower(root.get("name")),t),cb.like(cb.lower(root.get("gstin")),t),
                        cb.like(cb.lower(root.get("phone")),t)));
            }
            if(active!=null)p.add(cb.equal(root.get("active"),active));
            return cb.and(p.toArray(Predicate[]::new));
        },pageable).map(this::response);
    }
    @Transactional public VendorResponse createVendor(VendorRequest r){
        validateVendorUnique(r,null); Vendor v=new Vendor(); apply(v,r); auditCreate(v); return response(vendors.save(v));
    }
    @Transactional public VendorResponse updateVendor(Long id,VendorRequest r){
        Vendor v=vendors.findById(id).orElseThrow(()->new BusinessRuleException("VENDOR_NOT_FOUND","Vendor not found"));
        check(v.getVersion(),r.version(),Vendor.class,id); validateVendorUnique(r,id); apply(v,r);
        v.setUpdatedBy(auditor()); return response(vendors.save(v));
    }
    private void validatePartyUnique(PartyRequest r,Long id){
        String gst=upper(r.gstin()), pan=upper(r.pan());
        if(gst!=null&&(id==null?parties.existsByGstinIgnoreCase(gst):parties.existsByGstinIgnoreCaseAndIdNot(gst,id)))
            throw new BusinessRuleException("GSTIN_ALREADY_EXISTS","GSTIN already exists");
        if(pan!=null&&(id==null?parties.existsByPanIgnoreCase(pan):parties.existsByPanIgnoreCaseAndIdNot(pan,id)))
            throw new BusinessRuleException("PAN_ALREADY_EXISTS","PAN already exists");
    }
    private void validateVendorUnique(VendorRequest r,Long id){
        if(id==null?vendors.existsByNameIgnoreCase(r.name().trim()):vendors.existsByNameIgnoreCaseAndIdNot(r.name().trim(),id))
            throw new BusinessRuleException("VENDOR_NAME_ALREADY_EXISTS","Vendor name already exists");
        String gst=upper(r.gstin());
        if(gst!=null&&(id==null?vendors.existsByGstinIgnoreCase(gst):vendors.existsByGstinIgnoreCaseAndIdNot(gst,id)))
            throw new BusinessRuleException("GSTIN_ALREADY_EXISTS","GSTIN already exists");
    }
    private void apply(Party p,PartyRequest r){p.setLegalName(r.legalName().trim());p.setTradeName(trim(r.tradeName()));
        p.setGstin(upper(r.gstin()));p.setPan(upper(r.pan()));p.setContactPerson(trim(r.contactPerson()));
        p.setPhone(trim(r.phone()));p.setEmail(lower(r.email()));p.setAddress(trim(r.address()));p.setState(trim(r.state()));
        p.setNotes(trim(r.notes()));p.setActive(r.active());}
    private void apply(Vendor v,VendorRequest r){v.setName(r.name().trim());v.setGstin(upper(r.gstin()));
        v.setContactPerson(trim(r.contactPerson()));v.setPhone(trim(r.phone()));v.setEmail(lower(r.email()));
        v.setAddress(trim(r.address()));v.setNotes(trim(r.notes()));v.setActive(r.active());}
    private Party entity(Long id){return parties.findById(id).orElseThrow(()->new BusinessRuleException("PARTY_NOT_FOUND","Party not found"));}
    private PartyResponse response(Party p){return new PartyResponse(p.getId(),p.getLegalName(),p.getTradeName(),p.getGstin(),p.getPan(),
            p.getContactPerson(),p.getPhone(),p.getEmail(),p.getAddress(),p.getState(),p.getNotes(),p.isActive(),p.getVersion());}
    private VendorResponse response(Vendor v){return new VendorResponse(v.getId(),v.getName(),v.getGstin(),v.getContactPerson(),
            v.getPhone(),v.getEmail(),v.getAddress(),v.getNotes(),v.isActive(),v.getVersion());}
    private void auditCreate(com.stocksync.common.persistence.AuditedEntity e){e.setCreatedBy(auditor());e.setUpdatedBy(auditor());}
    private String auditor(){var a=SecurityContextHolder.getContext().getAuthentication();return a==null?"system":a.getName();}
    private String trim(String v){return v==null||v.isBlank()?null:v.trim();}
    private String upper(String v){String t=trim(v);return t==null?null:t.toUpperCase(Locale.ROOT);}
    private String lower(String v){String t=trim(v);return t==null?null:t.toLowerCase(Locale.ROOT);}
    private void check(long actual,Long supplied,Class<?> type,Long id){if(supplied==null||actual!=supplied)throw new ObjectOptimisticLockingFailureException(type,id);}
}
