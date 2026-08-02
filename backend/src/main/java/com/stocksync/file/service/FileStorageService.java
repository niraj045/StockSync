package com.stocksync.file.service;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.file.dto.FileAttachmentResponse;
import com.stocksync.file.entity.FileAttachment;
import com.stocksync.file.repository.FileAttachmentRepository;
import com.stocksync.inventory.repository.ItemRepository;
import com.stocksync.party.repository.*;
import com.stocksync.site.repository.SiteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class FileStorageService {
    private static final Set<String> TYPES=Set.of("application/pdf","application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","image/jpeg","image/png");
    private final FileAttachmentRepository files; private final PartyRepository parties; private final VendorRepository vendors;
    private final SiteRepository sites; private final ItemRepository items; private final Path root;
    public FileStorageService(FileAttachmentRepository files,PartyRepository parties,VendorRepository vendors,
            SiteRepository sites,ItemRepository items,@Value("${stocksync.file-storage-path}")String root){
        this.files=files;this.parties=parties;this.vendors=vendors;this.sites=sites;this.items=items;
        this.root=Paths.get(root).toAbsolutePath().normalize();
    }
    @Transactional
    public FileAttachmentResponse upload(String entityType,Long entityId,String documentType,String description,MultipartFile file){
        String type=entityType.toUpperCase(Locale.ROOT); validateEntity(type,entityId);
        if(file.isEmpty())throw new BusinessRuleException("FILE_EMPTY","Uploaded file is empty");
        if(file.getSize()>10L*1024*1024)throw new BusinessRuleException("FILE_TOO_LARGE","File must not exceed 10 MB");
        String content=Optional.ofNullable(file.getContentType()).orElse("application/octet-stream");
        if(!TYPES.contains(content))throw new BusinessRuleException("FILE_TYPE_NOT_ALLOWED","Allowed formats are PDF, DOCX, XLSX, JPG and PNG");
        String original=Paths.get(Optional.ofNullable(file.getOriginalFilename()).orElse("document")).getFileName().toString();
        String ext=original.contains(".")?original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT):"";
        String stored=UUID.randomUUID()+ext; Path directory=root.resolve(type.toLowerCase(Locale.ROOT)).resolve(entityId.toString()).normalize();
        Path destination=directory.resolve(stored).normalize();
        if(!destination.startsWith(root))throw new BusinessRuleException("INVALID_FILE_PATH","Invalid file path");
        try{Files.createDirectories(directory);file.transferTo(destination);}catch(IOException e){throw new BusinessRuleException("FILE_STORAGE_FAILED","Unable to store file");}
        FileAttachment a=new FileAttachment();a.setEntityType(type);a.setEntityId(entityId);a.setDocumentType(documentType.trim().toUpperCase(Locale.ROOT));
        a.setOriginalFilename(original);a.setStoredFilename(stored);a.setContentType(content);a.setFileSize(file.getSize());
        a.setStoragePath(root.relativize(destination).toString());a.setDescription(blank(description));a.setUploadedBy(auditor());
        return response(files.save(a));
    }
    @Transactional(readOnly=true)
    public List<FileAttachmentResponse> list(String entityType,Long entityId){
        return files.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType.toUpperCase(Locale.ROOT),entityId).stream().map(this::response).toList();
    }
    @Transactional(readOnly=true) public Download download(Long id){
        FileAttachment a=files.findById(id).orElseThrow(()->new BusinessRuleException("FILE_NOT_FOUND","File not found"));
        Path path=root.resolve(a.getStoragePath()).normalize();if(!path.startsWith(root)||!Files.isRegularFile(path))throw new BusinessRuleException("FILE_NOT_FOUND","Stored file not found");
        return new Download(new FileSystemResource(path),a.getOriginalFilename(),a.getContentType());
    }
    @Transactional
    public FileAttachment storeGenerated(String entityType,Long entityId,String documentType,String filename,byte[] content,String description){
        String type=entityType.toUpperCase(Locale.ROOT);String safe=Paths.get(filename).getFileName().toString();
        String stored=UUID.randomUUID()+".pdf";Path directory=root.resolve(type.toLowerCase(Locale.ROOT)).resolve(entityId.toString()).normalize();
        Path destination=directory.resolve(stored).normalize();
        if(!destination.startsWith(root))throw new BusinessRuleException("INVALID_FILE_PATH","Invalid file path");
        try{Files.createDirectories(directory);Files.write(destination,content,StandardOpenOption.CREATE_NEW);}catch(IOException e){throw new BusinessRuleException("FILE_STORAGE_FAILED","Unable to store generated PDF");}
        FileAttachment a=new FileAttachment();a.setEntityType(type);a.setEntityId(entityId);a.setDocumentType(documentType);
        a.setOriginalFilename(safe);a.setStoredFilename(stored);a.setContentType("application/pdf");a.setFileSize(content.length);
        a.setStoragePath(root.relativize(destination).toString());a.setDescription(blank(description));a.setUploadedBy(auditor());
        return files.save(a);
    }
    @Transactional(readOnly=true)
    public byte[] read(Long id){
        Download download=download(id);
        try{return download.resource().getInputStream().readAllBytes();}catch(IOException e){throw new BusinessRuleException("FILE_READ_FAILED","Unable to read stored file");}
    }
    private void validateEntity(String type,Long id){boolean exists=switch(type){
        case "PARTY"->parties.existsById(id);case "VENDOR"->vendors.existsById(id);case "SITE"->sites.existsById(id);case "ITEM"->items.existsById(id);
        default->throw new BusinessRuleException("INVALID_ENTITY_TYPE","Unsupported attachment entity type");};
        if(!exists)throw new BusinessRuleException(type+"_NOT_FOUND",type+" not found");}
    private FileAttachmentResponse response(FileAttachment a){return new FileAttachmentResponse(a.getId(),a.getEntityType(),a.getEntityId(),a.getDocumentType(),
            a.getOriginalFilename(),a.getContentType(),a.getFileSize(),a.getDescription(),a.getUploadedBy(),a.getCreatedAt());}
    private String auditor(){var a=SecurityContextHolder.getContext().getAuthentication();return a==null?"system":a.getName();}
    private String blank(String v){return v==null||v.isBlank()?null:v.trim();}
    public record Download(Resource resource,String filename,String contentType){}
}
