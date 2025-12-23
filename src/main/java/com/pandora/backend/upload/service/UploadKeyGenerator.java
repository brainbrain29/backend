package com.pandora.backend.upload.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class UploadKeyGenerator {

    private static final DateTimeFormatter DATE_SEGMENT_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private UploadKeyGenerator() {
    }

    public static String buildObjectKey(
            final String dirPrefix,
            final Integer userId,
            final String extension) {
        String safeExtension = extension;
        if (safeExtension == null || safeExtension.isBlank()) {
            safeExtension = "bin";
        }
        String dateSegment = LocalDate.now().format(DATE_SEGMENT_FORMATTER);
        String fileName = userId + "-" + System.currentTimeMillis() + "-"
                + UUID.randomUUID().toString().replace("-", "")
                + "." + safeExtension;
        return normalizePrefix(dirPrefix) + "staged/" + userId + "/" + dateSegment + "/" + fileName;
    }

    private static String normalizePrefix(final String dirPrefix) {
        if (dirPrefix == null || dirPrefix.isBlank()) {
            return "";
        }
        if (dirPrefix.endsWith("/")) {
            return dirPrefix;
        }
        return dirPrefix + "/";
    }
}
