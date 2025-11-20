package com.pandora.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 任务附件实体
 * 与 LogAttachment 结构相同，但关联到 Task
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "task")

@Entity
@Table(name = "task_attachment")
public class TaskAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, unique = true)
    private String storedFilename;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "upload_time", updatable = false)
    private LocalDateTime uploadTime;

    @Column(name = "uploaded_by")
    private Integer uploadedBy;

    @PrePersist
    protected void onCreate() {
        uploadTime = LocalDateTime.now();
    }
}
