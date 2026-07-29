package com.stocksync.agreement.service;

import com.stocksync.agreement.dto.AgreementTemplateResponse;
import com.stocksync.agreement.entity.AgreementTemplate;
import com.stocksync.agreement.entity.AgreementTemplateRenderingMode;
import com.stocksync.agreement.repository.AgreementTemplateRepository;
import com.stocksync.common.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class AgreementTemplateService {
    private final AgreementTemplateRepository repository;
    private final Path root;

    public AgreementTemplateService(AgreementTemplateRepository repository, @Value("${stocksync.file-storage-path}") String root) {
        this.repository = repository;
        this.root = Paths.get(root).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "agreementTemplates")
    public List<AgreementTemplateResponse> list() {
        return repository.findAllByOrderByNameAsc().stream()
                .map(this::response)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = "agreementTemplates", allEntries = true)
    public AgreementTemplateResponse upload(String name, String description, MultipartFile file) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("TEMPLATE_NAME_REQUIRED", "Template name is required");
        }
        if (repository.existsByNameIgnoreCase(name.trim())) {
            throw new BusinessRuleException("DUPLICATE_TEMPLATE_NAME", "Template name already exists");
        }
        if (file.isEmpty()) {
            throw new BusinessRuleException("FILE_EMPTY", "Uploaded file is empty");
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            throw new BusinessRuleException("FILE_TOO_LARGE", "File must not exceed 10 MB");
        }
        String contentType = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream");
        if (!contentType.equals("application/pdf") && !contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            throw new BusinessRuleException("FILE_TYPE_NOT_ALLOWED", "Allowed formats are PDF and DOCX");
        }

        String original = Paths.get(Optional.ofNullable(file.getOriginalFilename()).orElse("template")).getFileName().toString();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT) : "";
        String stored = UUID.randomUUID() + ext;

        // Save template to get database ID first
        AgreementTemplate template = new AgreementTemplate();
        template.setName(name.trim());
        template.setDescription(description == null || description.isBlank() ? null : description.trim());
        template.setRenderingMode(AgreementTemplateRenderingMode.REFERENCE);
        template.setOriginalFilename(original);
        template.setStoredFilename(stored);
        template.setContentType(contentType);
        template.setFileSize(file.getSize());
        template.setActive(true);
        // Temporary placeholder path
        template.setStoragePath("temp");
        template.setCreatedBy(actor());
        template.setUpdatedBy(actor());
        template = repository.save(template);

        Path directory = root.resolve("agreement-templates").resolve(String.valueOf(template.getId())).normalize();
        Path destination = directory.resolve(stored).normalize();
        if (!destination.startsWith(root)) {
            throw new BusinessRuleException("INVALID_FILE_PATH", "Invalid file path");
        }

        try {
            Files.createDirectories(directory);
            file.transferTo(destination);
        } catch (IOException e) {
            throw new BusinessRuleException("FILE_STORAGE_FAILED", "Unable to store template file");
        }

        template.setStoragePath(root.relativize(destination).toString());
        template = repository.save(template);

        return response(template);
    }

    @Transactional(readOnly = true)
    public Download download(Long id) {
        AgreementTemplate template = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("TEMPLATE_NOT_FOUND", "Agreement template not found"));
        if (template.getRenderingMode() == AgreementTemplateRenderingMode.NATIVE) {
            throw new BusinessRuleException("NATIVE_TEMPLATE_HAS_NO_SOURCE_FILE", "Built-in templates do not have a downloadable source file");
        }
        Path path = root.resolve(template.getStoragePath()).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            throw new BusinessRuleException("FILE_NOT_FOUND", "Stored template file not found");
        }
        return new Download(new FileSystemResource(path), template.getOriginalFilename(), template.getContentType());
    }

    private AgreementTemplateResponse response(AgreementTemplate t) {
        return new AgreementTemplateResponse(
                t.getId(),
                t.getTemplateCode(),
                t.getName(),
                t.getDescription(),
                t.getRenderingMode().name(),
                t.getLayoutKey(),
                t.isBuiltIn(),
                t.getTemplateVersion(),
                t.getOriginalFilename(),
                t.getContentType(),
                t.getFileSize(),
                t.isActive(),
                t.getVersion(),
                t.getCreatedAt()
        );
    }

    private String actor() {
        var a = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return a == null ? "system" : a.getName();
    }

    public record Download(Resource resource, String filename, String contentType) {}
}
