package com.pandora.backend.upload.constants;

public final class UploadConstants {

    public static final long SMALL_FILE_MAX_BYTES = 20L * 1024L * 1024L;
    public static final long MULTIPART_PART_SIZE_BYTES = 8L * 1024L * 1024L;
    public static final int MULTIPART_MAX_PARTS = 10_000;

    private UploadConstants() {
    }
}
