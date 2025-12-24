package com.pandora.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

@Service
public class PhoneSecurityService extends AbstractCryptoService {

    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;

    private final SecureRandom secureRandom;
    private final SecretKey encryptionKey;
    private final String hashPepper;

    public PhoneSecurityService(
            @Value("${app.security.phone.encryption-key-base64}") final String encryptionKeyBase64,
            @Value("${app.security.phone.hash-pepper:}") final String hashPepper) {
        this.secureRandom = new SecureRandom();
        this.encryptionKey = new SecretKeySpec(base64Decode(encryptionKeyBase64), "AES");
        this.hashPepper = hashPepper == null ? "" : hashPepper;
    }

    public String encryptPhone(final String plainPhone) {
        String normalized = normalizePhone(plainPhone);
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(utf8(normalized));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return base64(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Phone encryption failed", e);
        }
    }

    public String decryptPhone(final String phoneEnc) {
        if (phoneEnc == null || phoneEnc.isBlank()) {
            return null;
        }

        if (isLikelyPlainPhone(phoneEnc)) {
            return normalizePhone(phoneEnc);
        }

        byte[] combined;
        try {
            combined = base64Decode(phoneEnc);
        } catch (IllegalArgumentException e) {
            return normalizePhone(phoneEnc);
        }

        if (combined.length <= GCM_IV_LENGTH_BYTES) {
            return normalizePhone(phoneEnc);
        }

        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH_BYTES];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH_BYTES);
        System.arraycopy(combined, GCM_IV_LENGTH_BYTES, ciphertext, 0, ciphertext.length);

        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plainBytes = cipher.doFinal(ciphertext);
            return normalizePhone(new String(plainBytes));
        } catch (Exception e) {
            throw new IllegalStateException("Phone decryption failed", e);
        }
    }

    public String hashPhone(final String plainPhone) {
        String normalized = normalizePhone(plainPhone);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(utf8(hashPepper), "HmacSHA256");
            mac.init(secretKey);
            return toHex(mac.doFinal(utf8(normalized)));
        } catch (Exception e) {
            throw new IllegalStateException("Phone hash failed", e);
        }
    }

    public boolean isLegacyPlainPhoneEnc(final String phoneEnc) {
        if (phoneEnc == null) {
            return false;
        }
        return isLikelyPlainPhone(phoneEnc);
    }

    private static String normalizePhone(final String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private static boolean isLikelyPlainPhone(final String value) {
        String trimmed = value.trim();
        if (trimmed.length() < 6 || trimmed.length() > 32) {
            return false;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
