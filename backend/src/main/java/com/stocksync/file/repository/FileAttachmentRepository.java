package com.stocksync.file.repository;
import com.stocksync.file.entity.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface FileAttachmentRepository extends JpaRepository<FileAttachment,Long> {
    List<FileAttachment> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType,Long entityId);
}
