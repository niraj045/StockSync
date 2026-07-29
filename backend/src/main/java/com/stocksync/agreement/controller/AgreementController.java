package com.stocksync.agreement.controller;
import com.stocksync.agreement.dto.*;
import com.stocksync.agreement.entity.AgreementStatus;
import com.stocksync.agreement.service.AgreementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/agreements")
public class AgreementController {
 private final AgreementService service; public AgreementController(AgreementService service){this.service=service;}
 @GetMapping public Page<AgreementResponse> list(@RequestParam(required=false)String search,@RequestParam(required=false)String agreementNumber,
  @RequestParam(required=false)Long quotationId,@RequestParam(required=false)Long partyId,@RequestParam(required=false)Long siteId,
  @RequestParam(required=false)AgreementStatus status,
  @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate effectiveDateFrom,
  @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate effectiveDateTo,
  @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate expiryDateFrom,
  @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate expiryDateTo,Pageable pageable){
  return service.list(search,agreementNumber,quotationId,partyId,siteId,status,effectiveDateFrom,effectiveDateTo,expiryDateFrom,expiryDateTo,pageable);}
 @GetMapping("/{id}") public AgreementResponse get(@PathVariable Long id){return service.get(id);}
 @PostMapping("/from-quotation/{quotationId}") @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
 public AgreementResponse convert(@PathVariable Long quotationId,@Valid @RequestBody(required=false) QuotationConversionRequest body,HttpServletRequest h){
  return service.convert(quotationId,body,h);
 }
 @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
 public AgreementResponse update(@PathVariable Long id,@Valid @RequestBody AgreementRequest r,HttpServletRequest h){return service.update(id,r,h);}
 @PostMapping("/{id}/ready-for-review") @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')") public AgreementResponse ready(@PathVariable Long id,HttpServletRequest h){return service.ready(id,h);}
 @PostMapping("/{id}/return-to-draft") @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')") public AgreementResponse draft(@PathVariable Long id,@Valid @RequestBody AgreementReasonRequest r,HttpServletRequest h){return service.returnToDraft(id,r,h);}
 @PostMapping("/{id}/generate-document") @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')") public AgreementResponse generate(@PathVariable Long id,HttpServletRequest h){return service.generate(id,h);}
 @PostMapping("/{id}/activate") @PreAuthorize("hasRole('ADMIN')") public AgreementResponse activate(@PathVariable Long id,HttpServletRequest h){return service.activate(id,h);}
 @PostMapping("/{id}/expire") @PreAuthorize("hasRole('ADMIN')") public AgreementResponse expire(@PathVariable Long id,HttpServletRequest h){return service.expire(id,h);}
 @PostMapping("/{id}/terminate") @PreAuthorize("hasRole('ADMIN')") public AgreementResponse terminate(@PathVariable Long id,@Valid @RequestBody AgreementReasonRequest r,HttpServletRequest h){return service.terminate(id,r,h);}
 @PostMapping("/{id}/close") @PreAuthorize("hasRole('ADMIN')") public AgreementResponse close(@PathVariable Long id,HttpServletRequest h){return service.close(id,h);}
 @PostMapping("/{id}/cancel") @PreAuthorize("hasRole('ADMIN')") public AgreementResponse cancel(@PathVariable Long id,@Valid @RequestBody AgreementReasonRequest r,HttpServletRequest h){return service.cancel(id,r,h);}
 @GetMapping("/{id}/document") public ResponseEntity<Resource> document(@PathVariable Long id){var d=service.download(id);return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
  .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(d.filename(),StandardCharsets.UTF_8).build().toString()).body(d.resource());}
}
