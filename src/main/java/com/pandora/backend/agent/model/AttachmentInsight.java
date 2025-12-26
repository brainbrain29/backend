package com.pandora.backend.agent.model;

public record AttachmentInsight(
        Long attachmentId,
        String originalFilename,
        String fileType,
        String url,
        String summary,
        String error) {
}
