package com.stocksync.file.controller;
import com.stocksync.file.dto.FileAttachmentResponse;
import com.stocksync.file.service.FileStorageService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController @RequestMapping("/api/v1/files")
public class FileController {
    private final FileStorageService service;public FileController(FileStorageService service){this.service=service;}
    @GetMapping public List<FileAttachmentResponse> list(@RequestParam String entityType,@RequestParam Long entityId){return service.list(entityType,entityId);}
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("hasRole('ADMIN')")
    public FileAttachmentResponse upload(@RequestParam String entityType,@RequestParam Long entityId,
            @RequestParam @NotBlank String documentType,@RequestParam(required=false)String description,@RequestPart MultipartFile file){
        return service.upload(entityType,entityId,documentType,description,file);}
    @GetMapping("/{id}/download")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Long id){
        var d=service.download(id);return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(d.filename()).build().toString()).body(d.resource());}
}
