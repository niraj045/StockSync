package com.stocksync.agreement.controller;
import com.stocksync.agreement.dto.*;
import com.stocksync.agreement.entity.AgreementStatus;
import com.stocksync.agreement.service.AgreementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController @RequestMapping("/api/v1")
public class AgreementController {
    private final AgreementService service;public AgreementController(AgreementService service){this.service=service;}
    @GetMapping("/agreements")public Page<AgreementResponse> list(@RequestParam(required=false)String search,@RequestParam(required=false)AgreementStatus status,
        @RequestParam(required=false)Long partyId,@RequestParam(required=false)Long siteId,Pageable pageable){return service.list(search,status,partyId,siteId,pageable);}
    @GetMapping("/agreements/{id}")public AgreementResponse get(@PathVariable Long id){return service.get(id);}
    @PostMapping("/agreements")@PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public AgreementResponse create(@Valid @RequestBody AgreementRequest body,HttpServletRequest request){return service.create(body,request);}
    @PutMapping("/agreements/{id}")@PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public AgreementResponse update(@PathVariable Long id,@Valid @RequestBody AgreementRequest body,HttpServletRequest request){return service.update(id,body,request);}
    @PostMapping("/quotations/{id}/convert")@PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public AgreementResponse convert(@PathVariable Long id,@Valid @RequestBody QuotationConversionRequest body,HttpServletRequest request){return service.convert(id,body,request);}
    @PostMapping("/agreements/{id}/generate")@PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public AgreementResponse generate(@PathVariable Long id,HttpServletRequest request){return service.generate(id,request);}
    @PostMapping("/agreements/{id}/activate")@PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public AgreementResponse activate(@PathVariable Long id,HttpServletRequest request){return service.activate(id,request);}
    @PostMapping("/agreements/{id}/terminate")@PreAuthorize("hasRole('ADMIN')")
    public AgreementResponse terminate(@PathVariable Long id,HttpServletRequest request){return service.terminate(id,request);}
    @GetMapping("/agreements/{id}/document")public ResponseEntity<Resource> document(@PathVariable Long id){var d=service.downloadGenerated(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.contentType())).header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(d.filename(),StandardCharsets.UTF_8).build().toString()).body(d.resource());}
    @GetMapping("/agreement-templates")public List<AgreementTemplateResponse> templates(){return service.templates();}
    @PostMapping(value="/agreement-templates",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)@PreAuthorize("hasRole('ADMIN')")
    public AgreementTemplateResponse upload(@RequestParam String name,@RequestParam(required=false)String description,@RequestPart MultipartFile file,
            HttpServletRequest request){return service.uploadTemplate(name,description,file,request);}
    @GetMapping("/agreement-templates/{id}/download")public ResponseEntity<Resource> template(@PathVariable Long id){var d=service.downloadTemplate(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.contentType())).header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(d.filename(),StandardCharsets.UTF_8).build().toString()).body(d.resource());}
}
