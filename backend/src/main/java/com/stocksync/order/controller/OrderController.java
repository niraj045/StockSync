package com.stocksync.order.controller;
import com.stocksync.order.dto.*;
import com.stocksync.order.entity.OrderStatus;
import com.stocksync.order.service.OrderService;
import com.stocksync.order.service.OrderPdfService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController@RequestMapping("/api/v1/orders")
public class OrderController{
    private final OrderService service;private final OrderPdfService pdf;
    public OrderController(OrderService service,OrderPdfService pdf){this.service=service;this.pdf=pdf;}
    @GetMapping public Page<OrderResponse>list(@RequestParam(required=false)String search,@RequestParam(required=false)OrderStatus status,
        @RequestParam(required=false)Long agreementId,@RequestParam(required=false)Long siteId,Pageable pageable){return service.list(search,status,agreementId,siteId,pageable);}
    @GetMapping("/{id}")public OrderResponse get(@PathVariable Long id){return service.get(id);}
    @PostMapping@PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")public OrderResponse create(@Valid@RequestBody OrderRequest body,HttpServletRequest request){return service.create(body,request);}
    @PutMapping("/{id}")@PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")public OrderResponse update(@PathVariable Long id,@Valid@RequestBody OrderRequest body,HttpServletRequest request){return service.update(id,body,request);}
    @PostMapping("/{id}/confirm")@PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")public OrderResponse confirm(@PathVariable Long id,HttpServletRequest request){return service.confirm(id,request);}
    @PostMapping("/{id}/cancel")@PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")public OrderResponse cancel(@PathVariable Long id,HttpServletRequest request){return service.cancel(id,request);}
    @GetMapping("/{id}/pdf")@PreAuthorize("hasAnyRole('ADMIN','OPERATIONS','ACCOUNTS','VIEWER')")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id){
        var order = service.get(id);
        var doc = pdf.generate(order);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+doc.filename()+"\"").body(doc.content());
    }
}
