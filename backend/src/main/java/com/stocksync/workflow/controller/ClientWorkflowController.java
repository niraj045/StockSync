package com.stocksync.workflow.controller;

import com.stocksync.workflow.dto.ClientWorkflowDtos.*;
import com.stocksync.workflow.service.ClientWorkflowService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/client-workflow")
@PreAuthorize("hasAnyRole('ADMIN','OPERATIONS','ACCOUNTS')")
public class ClientWorkflowController {
    private final ClientWorkflowService service;
    public ClientWorkflowController(ClientWorkflowService service){this.service=service;}
    @GetMapping("/inquiries") public List<InquiryResponse> inquiries(@RequestParam(required=false)String search,@RequestParam(required=false)String status){return service.inquiries(search,status);}
    @PostMapping("/inquiries") @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')") public InquiryResponse createInquiry(@Valid @RequestBody InquiryRequest r){return service.createInquiry(r);}
    @PutMapping("/inquiries/{id}") @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')") public InquiryResponse updateInquiry(@PathVariable long id,@Valid @RequestBody InquiryRequest r){return service.updateInquiry(id,r);}
    @GetMapping("/operations") public List<OperationResponse> operations(@RequestParam(required=false)Long siteId,@RequestParam(required=false)String operationType,@RequestParam(required=false)String status){return service.operations(siteId,operationType,status);}
    @PostMapping("/operations") @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS','ACCOUNTS')") public OperationResponse createOperation(@Valid @RequestBody OperationRequest r){return service.createOperation(r);}
    @PutMapping("/operations/{id}") @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS','ACCOUNTS')") public OperationResponse updateOperation(@PathVariable long id,@Valid @RequestBody OperationRequest r){return service.updateOperation(id,r);}
}
