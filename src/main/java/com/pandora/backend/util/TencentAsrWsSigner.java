package com.pandora.backend.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.StringJoiner;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class TencentAsrWsSigner {

    private static final String HMAC_SHA1 = "HmacSHA1";

    private TencentAsrWsSigner() {
    }

    public static String buildSignedWsUrl(final String appId, final String secretKey,
            final Map<String, String> sortedQueryParams) {
        StringJoiner queryJoiner = new StringJoiner("&");
        for (Map.Entry<String, String> entry : sortedQueryParams.entrySet()) {
            queryJoiner.add(entry.getKey() + "=" + entry.getValue());
        }

        String hostPathWithQuery = "asr.cloud.tencent.com/asr/v2/" + appId + "?" + queryJoiner;
        String signatureRaw = base64(hmacSha1(hostPathWithQuery, secretKey));
        String signatureEncoded = URLEncoder.encode(signatureRaw, StandardCharsets.UTF_8);

        return "wss://" + hostPathWithQuery + "&signature=" + signatureEncoded;
    }

    private static byte[] hmacSha1(final String data, final String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA1);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA1);
            mac.init(secretKeySpec);
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign Tencent ASR ws url", e);
        }
    }

    private static String base64(final byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
