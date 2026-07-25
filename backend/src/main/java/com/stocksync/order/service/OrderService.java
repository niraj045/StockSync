package com.stocksync.order.service;
import com.stocksync.agreement.entity.Agreement;
import com.stocksync.agreement.repository.AgreementRepository;
import com.stocksync.agreement.service.AgreementAccess;
import com.stocksync.audit.service.UserActivityLogService;
import com.stocksync.auth.entity.User;
import com.stocksync.auth.repository.UserRepository;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.inventory.entity.Item;
import com.stocksync.inventory.repository.ItemRepository;
import com.stocksync.order.dto.*;
import com.stocksync.order.entity.*;
import com.stocksync.order.repository.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService{
    private final SiteOrderRepository orders;private final SiteOrderItemRepository orderItems;private final AgreementAccess agreementAccess;
    private final AgreementRepository agreementEntities;private final ItemRepository items;private final UserRepository users;private final UserActivityLogService audit;
    public OrderService(SiteOrderRepository orders,SiteOrderItemRepository orderItems,AgreementAccess agreementAccess,AgreementRepository agreementEntities,
        ItemRepository items,UserRepository users,UserActivityLogService audit){this.orders=orders;this.orderItems=orderItems;this.agreementAccess=agreementAccess;
        this.agreementEntities=agreementEntities;this.items=items;this.users=users;this.audit=audit;}
    @Transactional(readOnly=true)public Page<OrderResponse>list(String search,OrderStatus status,Long agreementId,Long siteId,Pageable pageable){
        return orders.findAll((root,q,cb)->{List<Predicate>p=new ArrayList<>();if(search!=null&&!search.isBlank()){String t="%"+search.trim().toLowerCase(Locale.ROOT)+"%";
            p.add(cb.or(cb.like(cb.lower(root.get("orderNumber")),t),cb.like(cb.lower(root.get("party").get("legalName")),t),cb.like(cb.lower(root.get("site").get("siteName")),t)));}
            if(status!=null)p.add(cb.equal(root.get("status"),status));if(agreementId!=null)p.add(cb.equal(root.get("agreement").get("id"),agreementId));
            if(siteId!=null)p.add(cb.equal(root.get("site").get("id"),siteId));return cb.and(p.toArray(Predicate[]::new));},pageable).map(this::response);}
    @Transactional(readOnly=true)public OrderResponse get(Long id){return response(detailed(id));}
    @Transactional public OrderResponse create(OrderRequest r,HttpServletRequest http){var agreement=agreementAccess.requireForOrder(r.agreementId());
        SiteOrder o=new SiteOrder();o.setOrderNumber(number());o.setStatus(OrderStatus.DRAFT);apply(o,r,agreement);
        o.setCreatedBy(actor());o.setUpdatedBy(actor());SiteOrder saved=orders.save(o);log("ORDER_DRAFT_CREATED",saved,"Draft created",http);return response(saved);}
    @Transactional public OrderResponse update(Long id,OrderRequest r,HttpServletRequest http){SiteOrder o=detailed(id);requireDraft(o);
        if(r.version()==null||o.getVersion()!=r.version())throw new ObjectOptimisticLockingFailureException(SiteOrder.class,id);
        var agreement=agreementAccess.requireForOrder(r.agreementId());apply(o,r,agreement);o.setUpdatedBy(actor());SiteOrder saved=orders.save(o);
        log("ORDER_DRAFT_UPDATED",saved,"Draft updated",http);return response(saved);}
    @Transactional public OrderResponse confirm(Long id,HttpServletRequest http){SiteOrder o=detailed(id);requireDraft(o);
        var agreement=agreementAccess.requireForOrder(o.getAgreement().getId());Map<Long,BigDecimal>limits=agreement.items().stream()
            .collect(Collectors.toMap(AgreementAccess.OrderAgreementItem::itemId,AgreementAccess.OrderAgreementItem::agreedQuantity));
        for(SiteOrderItem line:o.getItems()){BigDecimal allocated=orderItems.confirmedQuantity(agreement.id(),line.getItem().getId());
            BigDecimal limit=limits.get(line.getItem().getId());if(limit==null||allocated.add(line.getOrderedQuantity()).compareTo(limit)>0)
                throw new BusinessRuleException("AGREEMENT_QUANTITY_EXCEEDED","Confirmed orders exceed agreement quantity for "+line.getItem().getItemCode());}
        o.setStatus(OrderStatus.CONFIRMED);o.setUpdatedBy(actor());SiteOrder saved=orders.save(o);log("ORDER_CONFIRMED",saved,"Order confirmed",http);return response(saved);}
    @Transactional public OrderResponse cancel(Long id,HttpServletRequest http){SiteOrder o=detailed(id);
        if(o.getStatus()!=OrderStatus.DRAFT&&o.getStatus()!=OrderStatus.CONFIRMED)throw new BusinessRuleException("INVALID_ORDER_TRANSITION","Order cannot be cancelled");
        if(o.getItems().stream().anyMatch(i->i.getIssuedQuantity().signum()>0))throw new BusinessRuleException("ORDER_HAS_ISSUES","An order with issued material cannot be cancelled");
        o.setStatus(OrderStatus.CANCELLED);o.setUpdatedBy(actor());SiteOrder saved=orders.save(o);log("ORDER_CANCELLED",saved,"Order cancelled",http);return response(saved);}
    private void apply(SiteOrder o,OrderRequest r,AgreementAccess.OrderAgreement agreement){Set<Long>unique=new HashSet<>();Map<Long,BigDecimal>limits=agreement.items().stream()
        .collect(Collectors.toMap(AgreementAccess.OrderAgreementItem::itemId,AgreementAccess.OrderAgreementItem::agreedQuantity));List<SiteOrderItem>lines=new ArrayList<>();
        for(OrderItemRequest line:r.items()){if(!unique.add(line.itemId()))throw new BusinessRuleException("DUPLICATE_ITEM_LINE","Each item may appear only once");
            BigDecimal limit=limits.get(line.itemId());if(limit==null)throw new BusinessRuleException("ITEM_NOT_IN_AGREEMENT","Order item is not in the agreement");
            if(line.orderedQuantity().compareTo(limit)>0)throw new BusinessRuleException("AGREEMENT_QUANTITY_EXCEEDED","Order quantity exceeds agreement quantity");
            Item item=items.findById(line.itemId()).filter(Item::isActive).orElseThrow(()->new BusinessRuleException("ITEM_NOT_FOUND_OR_INACTIVE","Active item not found"));
            SiteOrderItem oi=new SiteOrderItem();oi.setItem(item);oi.setOrderedQuantity(line.orderedQuantity());oi.setIssuedQuantity(BigDecimal.ZERO);lines.add(oi);}
        Agreement entity=agreementEntities.getReferenceById(agreement.id());o.setAgreement(entity);o.setParty(entity.getParty());o.setSite(entity.getSite());
        o.setOrderDate(r.orderDate());o.setNotes(trim(r.notes()));o.replaceItems(lines);}
    private SiteOrder detailed(Long id){return orders.findDetailedById(id).orElseThrow(()->new BusinessRuleException("ORDER_NOT_FOUND","Order not found"));}
    private void requireDraft(SiteOrder o){if(o.getStatus()!=OrderStatus.DRAFT)throw new BusinessRuleException("ORDER_IMMUTABLE","Only draft orders can be edited");}
    private OrderResponse response(SiteOrder o){return new OrderResponse(o.getId(),o.getOrderNumber(),o.getAgreement().getId(),o.getAgreement().getAgreementNumber(),
        o.getParty().getId(),o.getParty().getLegalName(),o.getSite().getId(),o.getSite().getSiteName(),o.getOrderDate(),o.getStatus(),o.getNotes(),
        o.getItems().stream().map(i->new OrderItemResponse(i.getId(),i.getItem().getId(),i.getItem().getItemCode(),i.getItem().getItemName(),
            i.getItem().getUnit(),i.getOrderedQuantity(),i.getIssuedQuantity(),i.getRemainingQuantity(),i.getVersion())).toList(),o.getVersion(),o.getCreatedAt(),o.getUpdatedAt());}
    private String number(){return "ORD-"+LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)+"-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);}
    private String actor(){var a=SecurityContextHolder.getContext().getAuthentication();return a==null?"system":a.getName();}
    private String trim(String v){return v==null||v.isBlank()?null:v.trim();}
    private void log(String action,SiteOrder o,String description,HttpServletRequest req){User user=users.findByUsernameIgnoreCase(actor()).orElse(null);
        audit.log(user==null?null:user.getId(),actor(),action,"Order",String.valueOf(o.getId()),description,req);}
}
