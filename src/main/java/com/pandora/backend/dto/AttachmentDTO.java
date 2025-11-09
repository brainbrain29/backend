package com.pandora.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 附件 DTO
 * 用于返回给前端
 */
@Getter
@Setter
public class AttachmentDTO {

    private Integer attachmentId;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件访问URL
     * 格式: http://192.168.43.23:8080/api/attachments/{id}/preview
     */
    private String fileUrl;

    /**
     * 文件类型: image 或 document
     */
    private String fileType;

    /**
     * 文件分类: 1=图片, 2=文档
     */
    private Byte fileCategory;
}