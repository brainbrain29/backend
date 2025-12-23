package com.pandora.backend.upload.service;

import com.pandora.backend.upload.entity.UploadJob;
import com.pandora.backend.upload.enums.UploadJobStatus;
import com.pandora.backend.upload.repository.UploadJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class UploadStagingService {

    private static final int HEADER_BYTES_LENGTH = 16;

    private final UploadValidationService uploadValidationService;
    private final LocalTempFileService localTempFileService;
    private final UploadJobRepository uploadJobRepository;
    private final AsyncOssUploadService asyncOssUploadService;

    public UploadStagingService(
            UploadValidationService uploadValidationService,
            LocalTempFileService localTempFileService,
            UploadJobRepository uploadJobRepository,
            AsyncOssUploadService asyncOssUploadService) {
        this.uploadValidationService = uploadValidationService;
        this.localTempFileService = localTempFileService;
        this.uploadJobRepository = uploadJobRepository;
        this.asyncOssUploadService = asyncOssUploadService;
    }

    public UploadJob stageAndSubmit(
            final Integer userId,
            final MultipartFile file) {
        try {
            byte[] headerBytes = readHeaderBytes(file);
            String extension = uploadValidationService.validateAndGetExtension(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    headerBytes);

            Path localPath = saveToLocal(userId, file, extension, headerBytes);
            UploadJob job = createJob(userId, file, localPath);

            asyncOssUploadService.uploadAsync(job.getId(), extension);
            return job;
        } catch (Exception e) {
            throw new RuntimeException("Stage upload failed", e);
        }
    }

    private byte[] readHeaderBytes(final MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream()) {
            byte[] buffer = new byte[HEADER_BYTES_LENGTH];
            int read = is.read(buffer);
            if (read <= 0) {
                return new byte[0];
            }
            if (read == HEADER_BYTES_LENGTH) {
                return buffer;
            }
            byte[] actual = new byte[read];
            System.arraycopy(buffer, 0, actual, 0, read);
            return actual;
        }
    }

    private Path saveToLocal(
            final Integer userId,
            final MultipartFile file,
            final String extension,
            final byte[] headerBytes) throws IOException {
        try (InputStream is = file.getInputStream()) {
            return localTempFileService.saveToTempFile(userId, is, extension, headerBytes);
        }
    }

    private UploadJob createJob(
            final Integer userId,
            final MultipartFile file,
            final Path localPath) throws IOException {
        UploadJob job = new UploadJob();
        job.setUserId(userId);
        job.setOriginalFilename(file.getOriginalFilename());
        job.setContentType(file.getContentType());
        job.setFileSize(Files.size(localPath));
        job.setLocalPath(localPath.toString());
        job.setStatus(UploadJobStatus.UPLOAD_PENDING);
        return uploadJobRepository.save(job);
    }
}
