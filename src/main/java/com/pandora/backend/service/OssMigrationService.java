package com.pandora.backend.service;

import com.pandora.backend.entity.LogAttachment;
import com.pandora.backend.entity.TaskAttachment;
import com.pandora.backend.repository.LogAttachmentRepository;
import com.pandora.backend.repository.TaskAttachmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 迁移脚本：将本地文件迁移到 OSS
 * 
 * 警告：请在执行前备份数据库！
 */
@Slf4j
@Service
public class OssMigrationService implements CommandLineRunner {

    @Autowired
    private LogAttachmentRepository logAttachmentRepository;

    @Autowired
    private TaskAttachmentRepository taskAttachmentRepository;

    @Autowired
    private OssService ossService;

    @Value("${file.upload-dir}")
    private String localUploadDir;

    // 设置为 true 开启自动迁移，否则默认手动调用或不运行
    @Value("${migration.enable:false}")
    private boolean enableMigration;

    @Override
    public void run(String... args) throws Exception {
        if (!enableMigration) {
            log.info("Migration is disabled. Set 'migration.enable=true' to run.");
            return;
        }

        log.info("Starting migration from local storage to OSS...");
        migrateLogAttachments();
        migrateTaskAttachments();
        log.info("Migration completed!");
    }

    private void migrateLogAttachments() {
        List<LogAttachment> attachments = logAttachmentRepository.findAll();
        log.info("Found {} log attachments to migrate.", attachments.size());

        for (LogAttachment att : attachments) {
            if (att.getStoredFilename().contains("/")) {
                // 假设包含 / 已经是 OSS 路径了（或者之前的目录结构），简单判断防止重复迁移
                log.info("Skipping LogAttachment ID {} (Already migrated?)", att.getId());
                continue;
            }

            Path localFile = Paths.get(localUploadDir, att.getStoredFilename());
            if (!Files.exists(localFile)) {
                log.error("File not found for LogAttachment ID {}: {}", att.getId(), localFile);
                continue;
            }

            try (InputStream is = Files.newInputStream(localFile)) {
                String ossPath = ossService.uploadFileStream(is, att.getOriginalFilename());

                // 更新 DB
                att.setStoredFilename(ossPath);
                logAttachmentRepository.save(att);
                log.info("Migrated LogAttachment ID {} -> {}", att.getId(), ossPath);

            } catch (IOException e) {
                log.error("Failed to migrate LogAttachment ID {}", att.getId(), e);
            }
        }
    }

    private void migrateTaskAttachments() {
        List<TaskAttachment> attachments = taskAttachmentRepository.findAll();
        log.info("Found {} task attachments to migrate.", attachments.size());

        for (TaskAttachment att : attachments) {
            if (att.getStoredFilename().contains("/")) {
                log.info("Skipping TaskAttachment ID {} (Already migrated?)", att.getId());
                continue;
            }

            Path localFile = Paths.get(localUploadDir, att.getStoredFilename());
            if (!Files.exists(localFile)) {
                log.error("File not found for TaskAttachment ID {}: {}", att.getId(), localFile);
                continue;
            }

            try (InputStream is = Files.newInputStream(localFile)) {
                String ossPath = ossService.uploadFileStream(is, att.getOriginalFilename());

                // 更新 DB
                att.setStoredFilename(ossPath);
                taskAttachmentRepository.save(att);
                log.info("Migrated TaskAttachment ID {} -> {}", att.getId(), ossPath);

            } catch (IOException e) {
                log.error("Failed to migrate TaskAttachment ID {}", att.getId(), e);
            }
        }
    }
}
