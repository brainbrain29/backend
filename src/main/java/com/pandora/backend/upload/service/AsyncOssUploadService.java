package com.pandora.backend.upload.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;
import com.pandora.backend.upload.constants.UploadConstants;
import com.pandora.backend.upload.entity.UploadJob;
import com.pandora.backend.upload.enums.UploadJobStatus;
import com.pandora.backend.upload.repository.UploadJobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class AsyncOssUploadService {

    private final UploadJobRepository uploadJobRepository;
    private final OSS ossClient;

    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

    @Value("${aliyun.oss.dir-prefix:attachments/}")
    private String dirPrefix;

    public AsyncOssUploadService(final UploadJobRepository uploadJobRepository, final OSS ossClient) {
        this.uploadJobRepository = uploadJobRepository;
        this.ossClient = ossClient;
    }

    @Async("uploadExecutor")
    public void uploadAsync(final Long uploadJobId, final String extension) {
        UploadJob job = uploadJobRepository.findById(uploadJobId).orElse(null);
        if (job == null) {
            return;
        }

        try {
            markUploading(job);
            String objectKey = UploadKeyGenerator.buildObjectKey(dirPrefix, job.getUserId(), extension);
            uploadBySize(job, objectKey);
            markUploaded(job, objectKey);
            deleteLocalFile(job);
        } catch (Exception e) {
            markUploadFailed(job, e);
        }
    }

    private void uploadBySize(final UploadJob job, final String objectKey) throws Exception {
        if (job.getFileSize() != null && job.getFileSize() <= UploadConstants.SMALL_FILE_MAX_BYTES) {
            uploadSmallFile(job, objectKey);
            return;
        }
        uploadMultipart(job, objectKey);
    }

    private void uploadSmallFile(final UploadJob job, final String objectKey) throws Exception {
        Path localPath = Paths.get(job.getLocalPath());
        try (InputStream is = new FileInputStream(localPath.toFile())) {
            ossClient.putObject(bucketName, objectKey, is);
        }
    }

    private void uploadMultipart(final UploadJob job, final String objectKey) {
        File localFile = new File(job.getLocalPath());
        long fileLength = localFile.length();
        long partSize = UploadConstants.MULTIPART_PART_SIZE_BYTES;
        long partCountLong = (fileLength + partSize - 1) / partSize;
        if (partCountLong > UploadConstants.MULTIPART_MAX_PARTS) {
            throw new IllegalArgumentException("Too many parts: " + partCountLong);
        }

        String uploadId = initiateMultipart(objectKey);
        List<PartETag> partETags = new ArrayList<>();

        for (int i = 0; i < (int) partCountLong; i++) {
            long offset = i * partSize;
            long size = Math.min(partSize, fileLength - offset);
            partETags.add(uploadPart(objectKey, uploadId, localFile, offset, size, i + 1));
        }

        completeMultipart(objectKey, uploadId, partETags);
    }

    private String initiateMultipart(final String objectKey) {
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, objectKey);
        InitiateMultipartUploadResult result = ossClient.initiateMultipartUpload(request);
        return result.getUploadId();
    }

    private PartETag uploadPart(
            final String objectKey,
            final String uploadId,
            final File localFile,
            final long offset,
            final long size,
            final int partNumber) {
        try (FileInputStream fis = new FileInputStream(localFile)) {
            fis.getChannel().position(offset);

            UploadPartRequest request = new UploadPartRequest();
            request.setBucketName(bucketName);
            request.setKey(objectKey);
            request.setUploadId(uploadId);
            request.setInputStream(fis);
            request.setPartSize(size);
            request.setPartNumber(partNumber);

            UploadPartResult uploadPartResult = ossClient.uploadPart(request);
            return uploadPartResult.getPartETag();
        } catch (Exception e) {
            throw new RuntimeException("Upload part failed", e);
        }
    }

    private void completeMultipart(
            final String objectKey,
            final String uploadId,
            final List<PartETag> partETags) {
        CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest(
                bucketName,
                objectKey,
                uploadId,
                partETags);
        CompleteMultipartUploadResult completeResult = ossClient.completeMultipartUpload(completeRequest);
        if (completeResult == null) {
            throw new IllegalStateException("Multipart upload failed");
        }
    }

    private void markUploading(final UploadJob job) {
        job.setStatus(UploadJobStatus.UPLOADING);
        job.setErrorMessage(null);
        uploadJobRepository.save(job);
    }

    private void markUploaded(final UploadJob job, final String objectKey) {
        job.setStatus(UploadJobStatus.UPLOADED);
        job.setOssObjectKey(objectKey);
        job.setErrorMessage(null);
        uploadJobRepository.save(job);
    }

    private void markUploadFailed(final UploadJob job, final Exception e) {
        job.setStatus(UploadJobStatus.UPLOAD_FAILED);
        job.setErrorMessage(buildErrorMessage(e));
        uploadJobRepository.save(job);
    }

    private String buildErrorMessage(final Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return e.getClass().getSimpleName();
        }
        if (message.length() <= 1500) {
            return message;
        }
        return message.substring(0, 1500);
    }

    private void deleteLocalFile(final UploadJob job) throws Exception {
        Path localPath = Paths.get(job.getLocalPath());
        Files.deleteIfExists(localPath);
    }
}
