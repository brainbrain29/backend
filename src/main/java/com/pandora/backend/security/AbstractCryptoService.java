package com.pandora.backend.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

abstract class AbstractCryptoService {

    protected static String toHex(final byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    protected static byte[] utf8(final String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    protected static String base64(final byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    protected static byte[] base64Decode(final String value) {
        return Base64.getDecoder().decode(value);
    }
}
