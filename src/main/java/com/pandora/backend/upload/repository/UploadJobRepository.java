package com.pandora.backend.upload.repository;

import com.pandora.backend.upload.entity.UploadJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UploadJobRepository extends JpaRepository<UploadJob, Long> {
}
