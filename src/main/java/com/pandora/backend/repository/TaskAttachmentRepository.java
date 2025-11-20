package com.pandora.backend.repository;

import com.pandora.backend.entity.TaskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> {

    /**
     * 根据存储文件名查找附件
     */
    Optional<TaskAttachment> findByStoredFilename(String storedFilename);

    /**
     * 根据任务ID查找所有附件
     */
    List<TaskAttachment> findByTaskTaskId(Integer taskId);
}
