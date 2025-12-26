package com.pandora.backend.repository;

import com.pandora.backend.entity.LogAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogAttachmentRepository extends JpaRepository<LogAttachment, Long> {
    // 你可以添加一个通过 "安全文件名" 查找的方法，
    // 这在下载时很有用
    java.util.Optional<LogAttachment> findByStoredFilename(String storedFilename);

    List<LogAttachment> findByLogLogId(Integer logId);
}