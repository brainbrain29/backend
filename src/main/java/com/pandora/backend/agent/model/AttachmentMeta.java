package com.pandora.backend.agent.model;

import java.time.LocalDateTime;

public record AttachmentMeta(
        Long attachmentId,
        String originalFilename,
        String storedFilename,
        String fileType,
        Long fileSize,
        LocalDateTime uploadTime) {
}
