package com.pandora.backend.agent.service;

import com.pandora.backend.agent.model.AttachmentInsight;
import com.pandora.backend.agent.model.AttachmentMeta;
import com.pandora.backend.entity.Log;
import com.pandora.backend.entity.LogAttachment;
import com.pandora.backend.entity.Task;
import com.pandora.backend.entity.TaskAttachment;
import com.pandora.backend.repository.LogAttachmentRepository;
import com.pandora.backend.repository.LogRepository;
import com.pandora.backend.repository.TaskAttachmentRepository;
import com.pandora.backend.repository.TaskRepository;
import com.pandora.backend.service.GlmVisionClient;
import com.pandora.backend.service.OssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AttachmentInsightService {

    private static final int MAX_READ_ATTACHMENTS = 2;
    private static final int PRESIGNED_URL_EXPIRE_SECONDS = 300;

    private final LogRepository logRepository;
    private final TaskRepository taskRepository;
    private final LogAttachmentRepository logAttachmentRepository;
    private final TaskAttachmentRepository taskAttachmentRepository;
    private final OssService ossService;
    private final GlmVisionClient glmVisionClient;

    public List<AttachmentMeta> listLogAttachments(final Integer userId, final Integer logId) {
        final Optional<Log> logOpt = logRepository.findByIdWithDetails(logId);
        if (logOpt.isEmpty()) {
            return List.of();
        }

        final Log log = logOpt.get();
        if (log.getEmployee() == null || !log.getEmployee().getEmployeeId().equals(userId)) {
            return List.of();
        }

        final List<LogAttachment> attachments = logAttachmentRepository.findByLogLogId(logId);
        final List<AttachmentMeta> result = new ArrayList<>();
        for (LogAttachment att : attachments) {
            result.add(new AttachmentMeta(att.getId(), att.getOriginalFilename(), att.getStoredFilename(),
                    att.getFileType(),
                    att.getFileSize(), att.getUploadTime()));
        }
        return result;
    }

    public List<AttachmentMeta> listTaskAttachments(final Integer userId, final Integer taskId) {
        final Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            return List.of();
        }

        final Task task = taskOpt.get();
        final boolean isSender = task.getSender() != null && task.getSender().getEmployeeId().equals(userId);
        final boolean isAssignee = task.getAssignee() != null && task.getAssignee().getEmployeeId().equals(userId);
        if (!isSender && !isAssignee) {
            return List.of();
        }

        final List<TaskAttachment> attachments = taskAttachmentRepository.findByTaskTaskId(taskId);
        final List<AttachmentMeta> result = new ArrayList<>();
        for (TaskAttachment att : attachments) {
            result.add(new AttachmentMeta(att.getId(), att.getOriginalFilename(), att.getStoredFilename(),
                    att.getFileType(),
                    att.getFileSize(), att.getUploadTime()));
        }
        return result;
    }

    public List<AttachmentInsight> maybeAnalyzeAttachments(final List<AttachmentMeta> metas) {
        final List<AttachmentMeta> targets = selectTargets(metas);
        final List<AttachmentInsight> insights = new ArrayList<>();
        for (AttachmentMeta meta : targets) {
            insights.add(analyze(meta));
        }
        return insights;
    }

    private List<AttachmentMeta> selectTargets(final List<AttachmentMeta> metas) {
        if (metas.isEmpty()) {
            return List.of();
        }

        final List<AttachmentMeta> sorted = new ArrayList<>(metas);
        sorted.sort(Comparator.comparing(AttachmentMeta::uploadTime, Comparator.nullsLast(LocalDateTime::compareTo))
                .reversed());

        final List<AttachmentMeta> result = new ArrayList<>();
        for (AttachmentMeta meta : sorted) {
            if (result.size() >= MAX_READ_ATTACHMENTS) {
                break;
            }
            if (meta.fileType() == null) {
                continue;
            }
            if (meta.fileType().startsWith("image/") || meta.fileType().equalsIgnoreCase("application/pdf")) {
                result.add(meta);
            }
        }
        return result;
    }

    private AttachmentInsight analyze(final AttachmentMeta meta) {
        final String url;
        try {
            url = ossService.generatePresignedUrl(meta.storedFilename(), PRESIGNED_URL_EXPIRE_SECONDS);
        } catch (Exception e) {
            log.warn("[attachment-insight] presign failed attachmentId={} storedFilename={} fileType={}",
                    meta.attachmentId(), meta.storedFilename(), meta.fileType(), e);
            return new AttachmentInsight(meta.attachmentId(), meta.originalFilename(), meta.fileType(), null, null,
                    "OSS presign failed: " + e.getMessage());
        }

        final String prompt = "Please summarize this attachment. Focus on tasks, progress, and potential risks. "
                + "Return Chinese text within 150 characters.";

        try {
            final String summary = glmVisionClient.analyzeAttachmentUrl(url, meta.fileType(), prompt);
            return new AttachmentInsight(meta.attachmentId(), meta.originalFilename(), meta.fileType(), url, summary,
                    null);
        } catch (Exception e) {
            log.warn("[attachment-insight] vision analyze failed attachmentId={} fileType={} url={}",
                    meta.attachmentId(), meta.fileType(), url, e);
            return new AttachmentInsight(meta.attachmentId(), meta.originalFilename(), meta.fileType(), url, null,
                    "Vision analyze failed: " + e.getMessage());
        }
    }
}
