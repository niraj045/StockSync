package com.stocksync.inventory.controller;
import com.stocksync.inventory.dto.*;
import com.stocksync.inventory.service.InventoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;

@RestController @RequestMapping("/api/v1/stock")
public class StockController {
    private final InventoryService service;public StockController(InventoryService service){this.service=service;}
    @GetMapping("/balances")
    public Page<StockBalanceResponse> balances(@RequestParam(required=false)String search,
            @RequestParam(required=false)Boolean belowMinimum,Pageable pageable){return service.balancePage(search,belowMinimum,pageable);}
    @GetMapping("/balances/site/{siteId}")
    public Page<StockBalanceResponse> siteBalances(@PathVariable Long siteId, Pageable pageable){
        return service.siteBalancePage(siteId, pageable);
    }
    @GetMapping("/transactions")
    public Page<StockTransactionResponse> history(@RequestParam(required=false)Long itemId,@RequestParam(required=false)String type,
            @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate from,
            @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate to,Pageable pageable){
        return service.history(itemId,type,from,to,pageable);}
    @GetMapping("/summary") public Map<String,Object> summary(){return service.summary();}
    @PostMapping("/purchases") @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public InventoryDocumentResponse purchase(@RequestHeader("Idempotency-Key")String key,@Valid @RequestBody PurchaseRequest body,HttpServletRequest request){
        return service.purchase(key,body,request);}
    @PostMapping("/scrap") @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public InventoryDocumentResponse scrap(@RequestHeader("Idempotency-Key")String key,@Valid @RequestBody StockMovementRequest body,HttpServletRequest request){
        return service.scrap(key,body,request);}
    @PostMapping("/adjustments") @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public InventoryDocumentResponse adjustment(@RequestHeader("Idempotency-Key")String key,@Valid @RequestBody AdjustmentRequest body,HttpServletRequest request){
        return service.adjustment(key,body,request);}
}
