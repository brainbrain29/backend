package com.pandora.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 修改密码请求 DTO
 */
@Getter
@Setter
public class ChangePasswordDTO {
    private String oldPassword;

    /**
     * 新密码
     */
    private String newPassword;
}
