package com.pandora.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 日志附件实体
 * 使用 Blob 存储文件内容
 */
@Getter
@Setter
@Entity
@Table(name = "log_attachment")
public class LogAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Integer attachmentId;

    /**
     * 关联的日志
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id", nullable = false, foreignKey = @ForeignKey(name = "fk_log_attachment_log"))
    private Log log;

    /**
     * 原始文件名
     */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /**
     * 文件MIME类型
     * 例如: image/jpeg, image/png, application/pdf
     */
    @Column(name = "file_type", nullable = false, length = 100)
    private String fileType;

    /**
     * 文件大小(字节)
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /**
     * 文件内容(二进制)
     * 使用 @Lob 注解表示大对象
     * LONGBLOB 可以存储最大 4GB 的数据
     */
    @Lob
    @Column(name = "file_data", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] fileData;

    /**
     * 文件分类
     * 1 = 图片
     * 2 = 文档
     */
    @Column(name = "file_category", nullable = false)
    private Byte fileCategory;

    /**
     * 上传时间
     */
    @Column(name = "upload_time", nullable = false)
    private LocalDateTime uploadTime;

    /**
     * 上传者
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", foreignKey = @ForeignKey(name = "fk_log_attachment_employee"))
    private Employee uploadedBy;

    /**
     * 默认构造函数 (JPA 需要)
     */
    public LogAttachment() {
    }

    /**
     * 用于 JPQL 查询的构造函数(不加载 fileData)
     * 用于列表展示,避免加载大对象
     */
    public LogAttachment(Integer attachmentId, String fileName, String fileType,
            Long fileSize, Byte fileCategory, LocalDateTime uploadTime) {
        this.attachmentId = attachmentId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.fileCategory = fileCategory;
        this.uploadTime = uploadTime;
    }
}