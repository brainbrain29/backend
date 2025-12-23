package com.pandora.backend.upload.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Set;

@Service
public class UploadValidationService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg");

    private static final Map<String, Set<String>> ALLOWED_MIME_BY_EXTENSION = Map.of(
            "pdf", Set.of("application/pdf"),
            "png", Set.of("image/png"),
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg"));

    public String validateAndGetExtension(
            final String originalFilename,
            final String contentType,
            final byte[] headerBytes) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (extension == null) {
            throw new IllegalArgumentException("Missing file extension");
        }

        extension = extension.toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("File extension not allowed: " + extension);
        }

        validateMime(contentType, extension);
        validateMagicHeader(headerBytes, extension);
        return extension;
    }

    private void validateMime(final String contentType, final String extension) {
        if (contentType == null || contentType.isBlank()) {
            return;
        }
        Set<String> allowed = ALLOWED_MIME_BY_EXTENSION.get(extension);
        if (allowed == null || allowed.isEmpty()) {
            return;
        }
        if (!allowed.contains(contentType)) {
            throw new IllegalArgumentException("Mime type not allowed: " + contentType);
        }
    }

    private void validateMagicHeader(final byte[] headerBytes, final String extension) {
        if (headerBytes == null || headerBytes.length == 0) {
            throw new IllegalArgumentException("Empty file header");
        }

        boolean ok = switch (extension) {
            case "pdf" -> MagicHeaderSniffer.isPdf(headerBytes);
            case "png" -> MagicHeaderSniffer.isPng(headerBytes);
            case "jpg", "jpeg" -> MagicHeaderSniffer.isJpeg(headerBytes);
            default -> false;
        };

        if (!ok) {
            throw new IllegalArgumentException("File header does not match extension: " + extension);
        }
    }
}
