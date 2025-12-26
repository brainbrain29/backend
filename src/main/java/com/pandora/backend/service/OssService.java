package com.pandora.backend.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class OssService {

    @Autowired
    private OSS ossClient;

    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

    @Value("${aliyun.oss.dir-prefix}")
    private String dirPrefix;

    /**
     * 上传文件到 OSS
     *
     * @param file MultipartFile
     * @return 存储在 OSS 中的完整路径（Key）
     */
    public String uploadFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String storedFilename = dirPrefix + UUID.randomUUID().toString() + (extension != null ? "." + extension : "");

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectResult result = ossClient.putObject(bucketName, storedFilename, inputStream);
            log.info("File uploaded to OSS: {}, ETag: {}", storedFilename, result.getETag());
            return storedFilename;
        } catch (Exception e) {
            log.error("Failed to upload file to OSS", e);
            throw new IOException("OSS upload failed", e);
        }
    }

    /**
     * 上传本地文件流到 OSS (用于迁移)
     */
    public String uploadFileStream(InputStream inputStream, String originalFilename) throws IOException {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String storedFilename = dirPrefix + UUID.randomUUID().toString() + (extension != null ? "." + extension : "");

        try {
            ossClient.putObject(bucketName, storedFilename, inputStream);
            return storedFilename;
        } catch (Exception e) {
            log.error("Failed to upload stream to OSS", e);
            throw new IOException("OSS upload failed", e);
        }
    }

    /**
     * 从 OSS 获取文件资源
     */
    public Resource getFileAsResource(String storedFilename) {
        try {
            OSSObject ossObject = ossClient.getObject(bucketName, storedFilename);
            return new InputStreamResource(ossObject.getObjectContent());
        } catch (Exception e) {
            log.error("Failed to download file from OSS: {}", storedFilename, e);
            throw new RuntimeException("File not found in OSS: " + storedFilename);
        }
    }

    /**
     * 删除文件
     */
    public void deleteFile(String storedFilename) {
        try {
            ossClient.deleteObject(bucketName, storedFilename);
        } catch (Exception e) {
            log.error("Failed to delete file from OSS: {}", storedFilename, e);
        }
    }

    /**
     * 生成短暂的预签名 URL
     */
    public String generatePresignedUrl(final String storedFilename, final int expireSeconds) {
        try {
            final Instant expiresAt = Instant.now().plusSeconds(expireSeconds);
            final URL url = ossClient.generatePresignedUrl(bucketName, storedFilename, Date.from(expiresAt));
            return url.toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned url: {}", storedFilename, e);
            throw new RuntimeException("Failed to generate presigned url", e);
        }
    }
}
