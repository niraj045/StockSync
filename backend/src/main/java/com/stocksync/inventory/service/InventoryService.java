package com.stocksync.inventory.service;

import com.stocksync.audit.service.UserActivityLogService;
import com.stocksync.auth.entity.User;
import com.stocksync.auth.repository.UserRepository;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.inventory.dto.*;
import com.stocksync.inventory.entity.*;
import com.stocksync.inventory.repository.*;
import com.stocksync.party.repository.VendorRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class InventoryService {
    private final ItemRepository items;
    private final StockBalanceRepository balances;
    private final StockTransactionRepository transactions;
    private final SiteStockBalanceRepository siteBalances;
    private final VendorRepository vendors;
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final UserActivityLogService audit;

    public InventoryService(ItemRepository items, StockBalanceRepository balances,
            SiteStockBalanceRepository siteBalances,
            StockTransactionRepository transactions, VendorRepository vendors, JdbcTemplate jdbc,
            UserRepository users, UserActivityLogService audit) {
        this.items=items;this.balances=balances;this.siteBalances=siteBalances;this.transactions=transactions;this.vendors=vendors;
        this.jdbc=jdbc;this.users=users;this.audit=audit;
    }

    @Transactional
    public InventoryDocumentResponse purchase(String key, PurchaseRequest request, HttpServletRequest http) {
        validateKey(key); var replay=findDocument("purchases","purchase_number","purchase_date","total_value",key,"PURCHASE");
        if(replay!=null)return replay;
        if(!vendors.findById(request.vendorId()).filter(v->v.isActive()).isPresent())
            throw new BusinessRuleException("VENDOR_NOT_FOUND_OR_INACTIVE","Active vendor not found");
        validateLines(request.items(),true);
        String number=number("PUR",request.purchaseDate()); String actor=auditor();
        BigDecimal total=request.items().stream().map(l->l.quantity().multiply(l.unitRate())).reduce(BigDecimal.ZERO,BigDecimal::add);
        long id=insert("""
            INSERT INTO purchases(purchase_number,vendor_id,purchase_date,notes,total_value,idempotency_key,created_by)
            VALUES(?,?,?,?,?,?,?)
            """,number,request.vendorId(),request.purchaseDate(),blank(request.notes()),total,key,actor);
        try {
            for(StockLineRequest line:sorted(request.items())){
                Item item=activeItem(line.itemId()); BigDecimal lineValue=line.quantity().multiply(line.unitRate());
                jdbc.update("INSERT INTO purchase_items(purchase_id,item_id,quantity,unit_rate,line_value) VALUES(?,?,?,?,?)",
                        id,item.getId(),line.quantity(),line.unitRate(),lineValue);
                apply(item,line.quantity(),true,false,"PURCHASE",request.purchaseDate(),"PURCHASE",id,request.notes(),actor);
            }
        } catch (DuplicateKeyException ex) { throw new BusinessRuleException("DUPLICATE_POSTING","Purchase was already posted"); }
        audit("STOCK_PURCHASE_POSTED","Purchase",id,number,http);
        return new InventoryDocumentResponse(id,number,"PURCHASE",request.purchaseDate(),total,false);
    }

    @Transactional
    public InventoryDocumentResponse scrap(String key, StockMovementRequest request, HttpServletRequest http) {
        validateKey(key);var replay=findDocument("scrap_entries","scrap_number","scrap_date","NULL",key,"SCRAP");
        if(replay!=null)return replay;validateLines(request.items(),false);
        String number=number("SCR",request.date());String actor=auditor();
        long id=insert("INSERT INTO scrap_entries(scrap_number,scrap_date,reason,notes,idempotency_key,created_by) VALUES(?,?,?,?,?,?)",
                number,request.date(),request.reason().trim(),blank(request.notes()),key,actor);
        for(StockLineRequest line:sorted(request.items())){
            Item item=activeItem(line.itemId());
            jdbc.update("INSERT INTO scrap_items(scrap_entry_id,item_id,quantity) VALUES(?,?,?)",id,item.getId(),line.quantity());
            apply(item,line.quantity(),false,true,"SCRAP",request.date(),"SCRAP",id,request.reason(),actor);
        }
        audit("STOCK_SCRAP_POSTED","Scrap",id,number,http);
        return new InventoryDocumentResponse(id,number,"SCRAP",request.date(),null,false);
    }

    @Transactional
    public InventoryDocumentResponse adjustment(String key, AdjustmentRequest request, HttpServletRequest http) {
        validateKey(key);var replay=findDocument("stock_adjustments","adjustment_number","adjustment_date","NULL",key,"ADJUSTMENT");
        if(replay!=null)return replay;validateLines(request.items(),false);
        String number=number("ADJ",request.date());String actor=auditor();
        long id=insert("INSERT INTO stock_adjustments(adjustment_number,adjustment_date,direction,reason,notes,idempotency_key,created_by) VALUES(?,?,?,?,?,?,?)",
                number,request.date(),request.direction().name(),request.reason().trim(),blank(request.notes()),key,actor);
        boolean inward=request.direction()==AdjustmentRequest.Direction.IN;
        for(StockLineRequest line:sorted(request.items())){
            Item item=activeItem(line.itemId());
            jdbc.update("INSERT INTO stock_adjustment_items(adjustment_id,item_id,quantity) VALUES(?,?,?)",id,item.getId(),line.quantity());
            apply(item,line.quantity(),inward,false,inward?"ADJUSTMENT_IN":"ADJUSTMENT_OUT",request.date(),"ADJUSTMENT",id,request.reason(),actor);
        }
        audit("STOCK_ADJUSTMENT_POSTED","Adjustment",id,number,http);
        return new InventoryDocumentResponse(id,number,"ADJUSTMENT_"+request.direction(),request.date(),null,false);
    }

    @Transactional(readOnly=true)
    public Page<StockBalanceResponse> balancePage(String search,Boolean belowMinimum,Pageable pageable){
        return balances.findAll((root,q,cb)->{
            List<Predicate> p=new ArrayList<>();
            if(search!=null&&!search.isBlank()){String t="%"+search.trim().toLowerCase(Locale.ROOT)+"%";
                p.add(cb.or(cb.like(cb.lower(root.get("item").get("itemCode")),t),cb.like(cb.lower(root.get("item").get("itemName")),t)));}
            if(Boolean.TRUE.equals(belowMinimum))p.add(cb.lessThan(root.get("availableQuantity"),root.get("item").get("minimumStock")));
            return cb.and(p.toArray(Predicate[]::new));
        },pageable).map(this::balanceResponse);
    }

    @Transactional(readOnly=true)
    public Page<StockBalanceResponse> siteBalancePage(Long siteId, Pageable pageable){
        return siteBalances.findBySiteId(siteId, pageable).map(sb -> {
            Item i = sb.getItem();
            BigDecimal minimum = i.getMinimumStock();
            return new StockBalanceResponse(
                i.getId(),
                i.getItemCode(),
                i.getItemName(),
                i.getCategory() == null ? null : i.getCategory().getName(),
                i.getUnit(),
                sb.getPendingQuantity(),
                sb.getPendingQuantity(),
                sb.getPendingQuantity(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                sb.getPendingQuantity().multiply(Optional.ofNullable(i.getWeightPerPiece()).orElse(BigDecimal.ZERO)),
                minimum,
                false,
                sb.getVersion(),
                sb.getUpdatedAt()
            );
        });
    }

    @Transactional(readOnly=true)
    public Page<StockTransactionResponse> history(Long itemId,String type,LocalDate from,LocalDate to,Pageable pageable){
        return transactions.findAll((root,q,cb)->{List<Predicate> p=new ArrayList<>();
            if(itemId!=null)p.add(cb.equal(root.get("item").get("id"),itemId));
            if(type!=null&&!type.isBlank())p.add(cb.equal(root.get("transactionType"),type));
            if(from!=null)p.add(cb.greaterThanOrEqualTo(root.get("transactionDate"),from));
            if(to!=null)p.add(cb.lessThanOrEqualTo(root.get("transactionDate"),to));
            return cb.and(p.toArray(Predicate[]::new));},pageable).map(this::transactionResponse);
    }

    @Transactional(readOnly=true)
    public Map<String,Object> summary(){
        BigDecimal available=jdbc.queryForObject("SELECT COALESCE(SUM(available_quantity),0) FROM stock_balances",BigDecimal.class);
        BigDecimal scrapped=jdbc.queryForObject("SELECT COALESCE(SUM(scrapped_quantity),0) FROM stock_balances",BigDecimal.class);
        Long itemsCount=jdbc.queryForObject("SELECT COUNT(*) FROM items WHERE active=TRUE",Long.class);
        Long low=jdbc.queryForObject("SELECT COUNT(*) FROM stock_balances b JOIN items i ON i.id=b.item_id WHERE b.available_quantity<i.minimum_stock",Long.class);
        return Map.of("availableQuantity",available,"scrappedQuantity",scrapped,"activeItems",itemsCount,"belowMinimumItems",low);
    }

    private void apply(Item item,BigDecimal quantity,boolean inward,boolean scrap,String transactionType,
            LocalDate date,String sourceType,long sourceId,String notes,String actor){
        StockBalance balance=balances.findForUpdate(item.getId()).orElseGet(()->{
            jdbc.update("INSERT IGNORE INTO stock_balances(item_id) VALUES(?)",item.getId());
            return balances.findForUpdate(item.getId()).orElseThrow();
        });
        BigDecimal next=inward?balance.getAvailableQuantity().add(quantity):balance.getAvailableQuantity().subtract(quantity);
        if(next.signum()<0)throw new BusinessRuleException("INSUFFICIENT_STOCK",
                "Available stock for "+item.getItemCode()+" is "+balance.getAvailableQuantity()+", requested "+quantity);
        balance.setAvailableQuantity(next);
        if(scrap)balance.setScrappedQuantity(balance.getScrappedQuantity().add(quantity));
        BigDecimal weight=quantity.multiply(Optional.ofNullable(item.getWeightPerPiece()).orElse(BigDecimal.ZERO));
        balance.setAvailableWeight(next.multiply(Optional.ofNullable(item.getWeightPerPiece()).orElse(BigDecimal.ZERO)));
        balances.save(balance);
        StockTransaction transaction=new StockTransaction();transaction.setItem(item);transaction.setTransactionType(transactionType);
        transaction.setTransactionDate(date);transaction.setQuantity(quantity);transaction.setWeight(weight);
        transaction.setDirection(inward?"IN":"OUT");transaction.setSourceType(sourceType);transaction.setSourceId(sourceId);
        transaction.setNotes(blank(notes));transaction.setCreatedBy(actor);transactions.save(transaction);
    }
    private void validateLines(List<StockLineRequest> lines,boolean rateRequired){
        Set<Long> ids=new HashSet<>();for(var line:lines){if(!ids.add(line.itemId()))
            throw new BusinessRuleException("DUPLICATE_ITEM_LINE","Each item may appear only once");
            if(rateRequired&&line.unitRate()==null)throw new BusinessRuleException("UNIT_RATE_REQUIRED","Unit rate is required for purchases");}
    }
    private List<StockLineRequest> sorted(List<StockLineRequest> lines){return lines.stream().sorted(Comparator.comparing(StockLineRequest::itemId)).toList();}
    private Item activeItem(Long id){return items.findById(id).filter(Item::isActive).orElseThrow(()->new BusinessRuleException("ITEM_NOT_FOUND_OR_INACTIVE","Active item not found"));}
    private long insert(String sql,Object... values){GeneratedKeyHolder key=new GeneratedKeyHolder();
        jdbc.update(connection->{PreparedStatement ps=connection.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
            for(int i=0;i<values.length;i++)ps.setObject(i+1,values[i]);return ps;},key);
        return Objects.requireNonNull(key.getKey()).longValue();}
    private InventoryDocumentResponse findDocument(String table,String numberColumn,String dateColumn,String totalColumn,String key,String type){
        List<InventoryDocumentResponse> found=jdbc.query("SELECT id,"+numberColumn+","+dateColumn+","+totalColumn+" FROM "+table+" WHERE idempotency_key=?",
            (rs,n)->new InventoryDocumentResponse(rs.getLong(1),rs.getString(2),type,rs.getDate(3).toLocalDate(),
                    totalColumn.equals("NULL")?null:rs.getBigDecimal(4),true),key);return found.isEmpty()?null:found.getFirst();}
    private StockBalanceResponse balanceResponse(StockBalance b){Item i=b.getItem();BigDecimal minimum=i.getMinimumStock();
        return new StockBalanceResponse(i.getId(),i.getItemCode(),i.getItemName(),
        i.getCategory().getName(),i.getUnit(),b.getAvailableQuantity(),b.getIssuedQuantity(),b.getHiredQuantity(),b.getLostQuantity(),
        b.getScrappedQuantity(),b.getAvailableWeight(),minimum,minimum!=null&&b.getAvailableQuantity().compareTo(minimum)<0,b.getVersion(),b.getUpdatedAt());}
    private StockTransactionResponse transactionResponse(StockTransaction t){Item i=t.getItem();return new StockTransactionResponse(t.getId(),i.getId(),i.getItemCode(),
        i.getItemName(),t.getTransactionType(),t.getTransactionDate(),t.getQuantity(),t.getWeight(),t.getDirection(),t.getSourceType(),t.getSourceId(),
        t.getNotes(),t.getCreatedBy(),t.getCreatedAt());}
    private void audit(String action,String entity,long id,String description,HttpServletRequest request){User user=users.findByUsernameIgnoreCase(auditor()).orElse(null);
        audit.log(user==null?null:user.getId(),auditor(),action,entity,String.valueOf(id),description,request);}
    private String number(String prefix,LocalDate date){return prefix+"-"+date.format(DateTimeFormatter.BASIC_ISO_DATE)+"-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);}
    private void validateKey(String key){if(key==null||key.isBlank()||key.length()>100)throw new BusinessRuleException("IDEMPOTENCY_KEY_REQUIRED","A valid Idempotency-Key header is required");}
    private String auditor(){var a=SecurityContextHolder.getContext().getAuthentication();return a==null?"system":a.getName();}
    private String blank(String value){return value==null||value.isBlank()?null:value.trim();}
}
