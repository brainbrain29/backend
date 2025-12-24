package com.pandora.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDirString) {
        if (!StringUtils.hasText(uploadDirString)) {
            throw new IllegalStateException("file.upload-dir must not be blank");
        }
        this.uploadDir = Paths.get(uploadDirString);
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("无法初始化文件存储目录!", e);
        }
    }

    /**
     * 存储文件并返回唯一的存储文件名
     */
    public String storeFile(MultipartFile file) throws IOException {
        String originalFilenameRaw = file.getOriginalFilename();
        if (originalFilenameRaw == null || originalFilenameRaw.trim().isEmpty()) {
            throw new IOException("文件名不能为空");
        }
        String originalFilename = StringUtils.cleanPath(originalFilenameRaw);

        if (originalFilename.contains("..")) {
            throw new IOException("非法的文件名: " + originalFilename);
        }

        String extension = StringUtils.getFilenameExtension(originalFilename);
        String storedFilename = StringUtils.hasText(extension)
                ? UUID.randomUUID().toString() + "." + extension
                : UUID.randomUUID().toString();

        Path targetLocation = this.uploadDir.resolve(storedFilename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        }
        return storedFilename;
    }

    /**
     * 加载文件以供下载
     */
    public Resource loadFileAsResource(String storedFilename) {
        try {
            Path filePath = this.uploadDir.resolve(storedFilename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("文件未找到: " + storedFilename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("文件路径错误: " + storedFilename, e);
        }
    }
}