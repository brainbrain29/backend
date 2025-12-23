package com.pandora.backend.upload.service;

public final class MagicHeaderSniffer {

    private MagicHeaderSniffer() {
    }

    public static boolean isPdf(final byte[] headerBytes) {
        if (headerBytes.length < 5) {
            return false;
        }
        return headerBytes[0] == '%' && headerBytes[1] == 'P' && headerBytes[2] == 'D' && headerBytes[3] == 'F'
                && headerBytes[4] == '-';
    }

    public static boolean isPng(final byte[] headerBytes) {
        if (headerBytes.length < 8) {
            return false;
        }
        return (headerBytes[0] & 0xFF) == 0x89 && headerBytes[1] == 0x50 && headerBytes[2] == 0x4E
                && headerBytes[3] == 0x47 && headerBytes[4] == 0x0D && headerBytes[5] == 0x0A
                && headerBytes[6] == 0x1A && headerBytes[7] == 0x0A;
    }

    public static boolean isJpeg(final byte[] headerBytes) {
        if (headerBytes.length < 3) {
            return false;
        }
        return (headerBytes[0] & 0xFF) == 0xFF && (headerBytes[1] & 0xFF) == 0xD8 && (headerBytes[2] & 0xFF) == 0xFF;
    }
}
