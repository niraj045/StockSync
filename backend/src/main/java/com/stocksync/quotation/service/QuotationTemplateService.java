package com.stocksync.quotation.service;

import com.stocksync.audit.service.UserActivityLogService;
import com.stocksync.auth.repository.UserRepository;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.file.repository.FileAttachmentRepository;
import com.stocksync.quotation.dto.*;
import com.stocksync.quotation.entity.QuotationTemplate;
import com.stocksync.quotation.repository.QuotationTemplateRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuotationTemplateService {
    private final QuotationTemplateRepository templates;
    private final FileAttachmentRepository attachments;
    private final UserRepository users;
    private final UserActivityLogService audit;

    public QuotationTemplateService(QuotationTemplateRepository templates, FileAttachmentRepository attachments,
            UserRepository users, UserActivityLogService audit) {
        this.templates=templates; this.attachments=attachments; this.users=users; this.audit=audit;
    }

    @Transactional(readOnly=true)
    public Page<QuotationTemplateResponse> list(String search, Boolean active, Pageable pageable) {
        return templates.findAll((root,q,cb)->{
            List<Predicate> p=new ArrayList<>();
            if(search!=null&&!search.isBlank()){String t="%"+search.trim().toLowerCase(Locale.ROOT)+"%";
                p.add(cb.or(cb.like(cb.lower(root.get("templateCode")),t),cb.like(cb.lower(root.get("name")),t)));}
            if(active!=null)p.add(cb.equal(root.get("active"),active));
            return cb.and(p.toArray(Predicate[]::new));
        },pageable).map(this::response);
    }
    @Transactional(readOnly=true) public QuotationTemplateResponse get(Long id){return response(required(id));}
    @Transactional public QuotationTemplateResponse create(QuotationTemplateRequest r,HttpServletRequest req){
        if(templates.existsByTemplateCodeIgnoreCase(r.templateCode().trim())) duplicate();
        QuotationTemplate t=new QuotationTemplate(); apply(t,r); t.setCreatedBy(actor());t.setUpdatedBy(actor());
        t=templates.save(t); log("QUOTATION_TEMPLATE_CREATED",t,req); return response(t);
    }
    @Transactional public QuotationTemplateResponse update(Long id,QuotationTemplateRequest r,HttpServletRequest req){
        QuotationTemplate t=required(id);
        if(r.version()==null||t.getVersion()!=r.version())throw new ObjectOptimisticLockingFailureException(QuotationTemplate.class,id);
        if(templates.existsByTemplateCodeIgnoreCaseAndIdNot(r.templateCode().trim(),id))duplicate();
        apply(t,r);t.setUpdatedBy(actor());t=templates.save(t);log("QUOTATION_TEMPLATE_UPDATED",t,req);return response(t);
    }
    @Transactional public QuotationTemplateResponse active(Long id,boolean active,HttpServletRequest req){
        QuotationTemplate t=required(id);t.setActive(active);t.setUpdatedBy(actor());t=templates.save(t);
        log(active?"QUOTATION_TEMPLATE_ACTIVATED":"QUOTATION_TEMPLATE_DEACTIVATED",t,req);return response(t);
    }
    @Transactional(readOnly=true) public QuotationTemplate requireActive(Long id){
        return templates.findByIdAndActiveTrue(id).orElseThrow(()->new BusinessRuleException("QUOTATION_TEMPLATE_INACTIVE","Active quotation template not found"));
    }
    private void apply(QuotationTemplate t,QuotationTemplateRequest r){
        t.setTemplateCode(r.templateCode().trim().toUpperCase(Locale.ROOT));t.setName(r.name().trim());
        t.setDescription(trim(r.description()));t.setCompanyName(trim(r.companyName()));t.setCompanyAddress(trim(r.companyAddress()));
        t.setCompanyGstin(trim(r.companyGstin()));t.setHeaderText(trim(r.headerText()));t.setFooterText(trim(r.footerText()));
        t.setDefaultTerms(trim(r.defaultTerms()));t.setDefaultNotes(trim(r.defaultNotes()));
        t.setLogoAttachment(r.logoAttachmentId()==null?null:attachments.findById(r.logoAttachmentId())
                .orElseThrow(()->new BusinessRuleException("ATTACHMENT_NOT_FOUND","Logo attachment not found")));
    }
    private QuotationTemplate required(Long id){return templates.findById(id).orElseThrow(()->new BusinessRuleException("QUOTATION_TEMPLATE_NOT_FOUND","Quotation template not found"));}
    private QuotationTemplateResponse response(QuotationTemplate t){return new QuotationTemplateResponse(t.getId(),t.getTemplateCode(),t.getName(),t.getDescription(),
        t.getCompanyName(),t.getCompanyAddress(),t.getCompanyGstin(),t.getHeaderText(),t.getFooterText(),t.getDefaultTerms(),t.getDefaultNotes(),
        t.getLogoAttachment()==null?null:t.getLogoAttachment().getId(),t.isActive(),t.getVersion(),t.getCreatedAt(),t.getCreatedBy(),t.getUpdatedAt(),t.getUpdatedBy());}
    private void duplicate(){throw new BusinessRuleException("DUPLICATE_TEMPLATE_CODE","Template code already exists");}
    private String trim(String v){return v==null||v.isBlank()?null:v.trim();}
    private String actor(){var a=SecurityContextHolder.getContext().getAuthentication();return a==null?"system":a.getName();}
    private void log(String action,QuotationTemplate t,HttpServletRequest req){var u=users.findByUsernameIgnoreCase(actor()).orElse(null);
        audit.log(u==null?null:u.getId(),actor(),action,"QuotationTemplate",String.valueOf(t.getId()),t.getTemplateCode(),req);}
}

