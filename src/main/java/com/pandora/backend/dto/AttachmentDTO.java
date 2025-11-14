package com.pandora.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttachmentDTO {
    private Long id; // 附件ID (用于生成下载链接)
    private String originalFilename; // 原始文件名
    private String fileType;
    private Long fileSize;
}