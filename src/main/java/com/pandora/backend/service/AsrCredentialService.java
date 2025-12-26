package com.pandora.backend.service;

import java.time.Instant;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.pandora.backend.config.TencentAsrConfig;
import com.pandora.backend.dto.AsrCredentialResponse;
import com.pandora.backend.util.TencentAsrWsSigner;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AsrCredentialService {

    private final TencentAsrConfig tencentAsrConfig;

    public AsrCredentialResponse generateCredential(final Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User is required");
        }

        String appId = tencentAsrConfig.getAppId();
        String secretId = tencentAsrConfig.getSecretId();
        String secretKey = tencentAsrConfig.getSecretKey();

        if (isBlank(appId) || isBlank(secretId) || isBlank(secretKey)) {
            throw new IllegalStateException("Tencent ASR is not configured");
        }

        long timestampSeconds = Instant.now().getEpochSecond();
        int expiresInSeconds = tencentAsrConfig.getExpiresInSeconds() != null ? tencentAsrConfig.getExpiresInSeconds()
                : 600;
        long expiredSeconds = timestampSeconds + expiresInSeconds;

        String voiceId = UUID.randomUUID().toString();
        long nonce = System.currentTimeMillis();

        TreeMap<String, String> params = new TreeMap<>();
        params.put("engine_model_type", tencentAsrConfig.getEngineModelType());
        params.put("expired", String.valueOf(expiredSeconds));
        params.put("needvad", String.valueOf(tencentAsrConfig.getNeedVad()));
        params.put("nonce", String.valueOf(nonce));
        params.put("secretid", secretId);
        params.put("timestamp", String.valueOf(timestampSeconds));
        params.put("voice_format", String.valueOf(tencentAsrConfig.getVoiceFormat()));
        params.put("voice_id", voiceId);

        String wsUrl = TencentAsrWsSigner.buildSignedWsUrl(appId, secretKey, params);

        AsrCredentialResponse response = new AsrCredentialResponse();
        response.setWsUrl(wsUrl);
        response.setExpiresIn(expiresInSeconds);
        response.setExpireAt(System.currentTimeMillis() + (expiresInSeconds * 1000L));
        response.setRequestId(UUID.randomUUID().toString());
        response.setVoiceId(voiceId);
        return response;
    }

    private static boolean isBlank(final String value) {
        return value == null || value.trim().isEmpty();
    }
}
