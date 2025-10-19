package com.pandora.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginDTO {
    private String phone;
    private String password;
}
