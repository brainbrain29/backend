package com.pandora.backend.upload.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class LocalTempFileService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public Path saveToTempFile(
            final Integer userId,
            final InputStream inputStream,
            final String extension,
            final byte[] headerBytes) throws IOException {
        Path dir = resolveUserTempDir(userId);
        Files.createDirectories(dir);

        String fileName = userId + "-" + System.currentTimeMillis() + "-"
                + UUID.randomUUID().toString().replace("-", "")
                + "." + extension;
        Path filePath = dir.resolve(fileName);

        try (BufferedInputStream bis = new BufferedInputStream(inputStream);
                OutputStream os = new BufferedOutputStream(Files.newOutputStream(filePath))) {
            os.write(headerBytes);
            bis.skip(headerBytes.length);
            bis.transferTo(os);
            os.flush();
        }

        return filePath;
    }

    private Path resolveUserTempDir(final Integer userId) {
        return Paths.get(uploadDir, "tmp", String.valueOf(userId));
    }
}
