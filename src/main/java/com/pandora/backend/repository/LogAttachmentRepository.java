package com.pandora.backend.repository;

import com.pandora.backend.entity.LogAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 日志附件 Repository
 */
@Repository
public interface LogAttachmentRepository extends JpaRepository<LogAttachment, Integer> {

    /**
     * 根据日志ID查询所有附件(不包含文件内容)
     * 用于列表展示,避免加载大对象
     */
    @Query("SELECT new com.pandora.backend.entity.LogAttachment(" +
            "a.attachmentId, a.fileName, a.fileType, a.fileSize, a.fileCategory, a.uploadTime) " +
            "FROM LogAttachment a WHERE a.log.logId = :logId")
    List<LogAttachment> findByLogIdWithoutData(@Param("logId") Integer logId);

    /**
     * 根据日志ID查询所有附件(包含文件内容)
     */
    List<LogAttachment> findByLogLogId(Integer logId);

    /**
     * 根据日志ID删除所有附件
     */
    void deleteByLogLogId(Integer logId);
}