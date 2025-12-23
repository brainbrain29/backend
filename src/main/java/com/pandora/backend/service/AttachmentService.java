package com.pandora.backend.service;

import com.pandora.backend.entity.LogAttachment;
import com.pandora.backend.entity.TaskAttachment;
import com.pandora.backend.repository.LogAttachmentRepository;
import com.pandora.backend.repository.TaskAttachmentRepository;
import com.pandora.backend.util.JwtUtil;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 通用附件服务
 * 复用日志附件和任务附件的下载、预览逻辑
 */
@Slf4j
@Service
public class AttachmentService {

    @Autowired
    private LogAttachmentRepository logAttachmentRepository;

    @Autowired
    private TaskAttachmentRepository taskAttachmentRepository;

    // @Autowired
    // private FileStorageService fileStorageService;
    @Autowired
    private OssService ossService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 从请求中提取 Token
     */
    private String extractToken(String tokenParam, HttpServletRequest request) {
        if (tokenParam != null && !tokenParam.isEmpty()) {
            return tokenParam;
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * 验证 Token 并返回用户 ID
     */
    private Integer validateAndExtractUserId(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        if (!jwtUtil.validateToken(token)) {
            return null;
        }
        return jwtUtil.extractUserId(token);
    }

    /**
     * 下载日志附件
     */
    public ResponseEntity<Resource> downloadLogAttachment(
            Long attachmentId,
            String tokenParam,
            HttpServletRequest request) {

        try {
            // 1. 验证 Token
            String token = extractToken(tokenParam, request);
            Integer userId = validateAndExtractUserId(token);
            if (userId == null) {
                log.warn("[日志附件下载] 401 UNAUTHORIZED - 附件ID: {}, 原因: Token无效", attachmentId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            // 2. 查找附件
            LogAttachment attachment = logAttachmentRepository.findById(attachmentId)
                    .orElseThrow(() -> {
                        log.warn("[日志附件下载] 404 NOT_FOUND - 附件ID: {}", attachmentId);
                        return new RuntimeException("附件未找到");
                    });

            // 3. 权限检查
            if (attachment.getLog() == null ||
                    !attachment.getLog().getEmployee().getEmployeeId().equals(userId)) {
                log.warn("[日志附件下载] 403 FORBIDDEN - 附件ID: {}, 用户ID: {}", attachmentId, userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }

            // 4. 加载文件并返回
            return buildDownloadResponse(
                    attachment.getStoredFilename(),
                    attachment.getOriginalFilename(),
                    attachment.getFileType(),
                    attachment.getFileSize());

        } catch (Exception e) {
            log.error("[日志附件下载] 500 ERROR - 附件ID: {}, 错误: {}", attachmentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 预览日志附件
     */
    public ResponseEntity<Resource> previewLogAttachment(
            Long attachmentId,
            String tokenParam,
            HttpServletRequest request) {

        try {
            String token = extractToken(tokenParam, request);
            Integer userId = validateAndExtractUserId(token);
            if (userId == null) {
                log.warn("[日志附件预览] 401 UNAUTHORIZED - 附件ID: {}", attachmentId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            LogAttachment attachment = logAttachmentRepository.findById(attachmentId)
                    .orElseThrow(() -> new RuntimeException("附件未找到"));

            if (attachment.getLog() == null ||
                    !attachment.getLog().getEmployee().getEmployeeId().equals(userId)) {
                log.warn("[日志附件预览] 403 FORBIDDEN - 附件ID: {}, 用户ID: {}", attachmentId, userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }

            return buildPreviewResponse(
                    attachment.getStoredFilename(),
                    attachment.getOriginalFilename(),
                    attachment.getFileType(),
                    attachment.getFileSize());

        } catch (Exception e) {
            log.error("[日志附件预览] 500 ERROR - 附件ID: {}, 错误: {}", attachmentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 下载任务附件
     */
    public ResponseEntity<Resource> downloadTaskAttachment(
            Long attachmentId,
            String tokenParam,
            HttpServletRequest request) {

        try {
            String token = extractToken(tokenParam, request);
            Integer userId = validateAndExtractUserId(token);
            if (userId == null) {
                log.warn("[任务附件下载] 401 UNAUTHORIZED - 附件ID: {}", attachmentId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            TaskAttachment attachment = taskAttachmentRepository.findById(attachmentId)
                    .orElseThrow(() -> {
                        log.warn("[任务附件下载] 404 NOT_FOUND - 附件ID: {}", attachmentId);
                        return new RuntimeException("附件未找到");
                    });

            // 权限检查：任务的发送者或执行者可以下载
            if (attachment.getTask() == null ||
                    (!attachment.getTask().getSender().getEmployeeId().equals(userId) &&
                            (attachment.getTask().getAssignee() == null ||
                                    !attachment.getTask().getAssignee().getEmployeeId().equals(userId)))) {
                log.warn("[任务附件下载] 403 FORBIDDEN - 附件ID: {}, 用户ID: {}", attachmentId, userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }

            return buildDownloadResponse(
                    attachment.getStoredFilename(),
                    attachment.getOriginalFilename(),
                    attachment.getFileType(),
                    attachment.getFileSize());

        } catch (Exception e) {
            log.error("[任务附件下载] 500 ERROR - 附件ID: {}, 错误: {}", attachmentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 预览任务附件
     */
    public ResponseEntity<Resource> previewTaskAttachment(
            Long attachmentId,
            String tokenParam,
            HttpServletRequest request) {

        try {
            String token = extractToken(tokenParam, request);
            Integer userId = validateAndExtractUserId(token);
            if (userId == null) {
                log.warn("[任务附件预览] 401 UNAUTHORIZED - 附件ID: {}", attachmentId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            TaskAttachment attachment = taskAttachmentRepository.findById(attachmentId)
                    .orElseThrow(() -> new RuntimeException("附件未找到"));

            if (attachment.getTask() == null ||
                    (!attachment.getTask().getSender().getEmployeeId().equals(userId) &&
                            (attachment.getTask().getAssignee() == null ||
                                    !attachment.getTask().getAssignee().getEmployeeId().equals(userId)))) {
                log.warn("[任务附件预览] 403 FORBIDDEN - 附件ID: {}, 用户ID: {}", attachmentId, userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }

            return buildPreviewResponse(
                    attachment.getStoredFilename(),
                    attachment.getOriginalFilename(),
                    attachment.getFileType(),
                    attachment.getFileSize());

        } catch (Exception e) {
            log.error("[任务附件预览] 500 ERROR - 附件ID: {}, 错误: {}", attachmentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 构建下载响应（通用方法）
     */
    private ResponseEntity<Resource> buildDownloadResponse(
            String storedFilename,
            String originalFilename,
            String fileType,
            Long fileSize) throws Exception {

        // Resource resource = fileStorageService.loadFileAsResource(storedFilename);
        Resource resource = ossService.getFileAsResource(storedFilename);

        String encodedFilename = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.parseMediaType(fileType))
                .contentLength(fileSize)
                .body(resource);
    }

    /**
     * 构建预览响应（通用方法）
     */
    private ResponseEntity<Resource> buildPreviewResponse(
            String storedFilename,
            String originalFilename,
            String fileType,
            Long fileSize) throws Exception {

        // Resource resource = fileStorageService.loadFileAsResource(storedFilename);
        Resource resource = ossService.getFileAsResource(storedFilename);

        String encodedFilename = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.parseMediaType(fileType))
                .contentLength(fileSize)
                .body(resource);
    }
}
