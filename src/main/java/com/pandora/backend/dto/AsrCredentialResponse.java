package com.pandora.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AsrCredentialResponse {

    private String wsUrl;

    private Long expireAt;

    private Integer expiresIn;

    private String requestId;

    private String voiceId;

    public AsrCredentialResponse() {
    }
}
