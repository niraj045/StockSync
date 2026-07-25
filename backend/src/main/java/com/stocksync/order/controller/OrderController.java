package com.stocksync.order.controller;
import com.stocksync.order.dto.*;
import com.stocksync.order.entity.OrderStatus;
import com.stocksync.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController@RequestMapping("/api/v1/orders")
public class OrderController{
    private final OrderService service;public OrderController(OrderService service){this.service=service;}
    @GetMapping public Page<OrderResponse>list(@RequestParam(required=false)String search,@RequestParam(required=false)OrderStatus status,
        @RequestParam(required=false)Long agreementId,@RequestParam(required=false)Long siteId,Pageable pageable){return service.list(search,status,agreementId,siteId,pageable);}
    @GetMapping("/{id}")public OrderResponse get(@PathVariable Long id){return service.get(id);}
    @PostMapping@PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")public OrderResponse create(@Valid@RequestBody OrderRequest body,HttpServletRequest request){return service.create(body,request);}
    @PutMapping("/{id}")@PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")public OrderResponse update(@PathVariable Long id,@Valid@RequestBody OrderRequest body,HttpServletRequest request){return service.update(id,body,request);}
    @PostMapping("/{id}/confirm")@PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")public OrderResponse confirm(@PathVariable Long id,HttpServletRequest request){return service.confirm(id,request);}
    @PostMapping("/{id}/cancel")@PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")public OrderResponse cancel(@PathVariable Long id,HttpServletRequest request){return service.cancel(id,request);}
}
