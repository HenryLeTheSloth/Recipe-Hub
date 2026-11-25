package com.recipehub.media;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/** Nhận diện ảnh theo magic bytes — không tin tên file hay Content-Type của client. */
public final class MediaTypes {

    static final int HEAD = 12;
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    private MediaTypes() {
    }

    public static Optional<String> detect(byte[] head) {
        if (startsWith(head, JPEG)) return Optional.of("image/jpeg");
        if (startsWith(head, PNG)) return Optional.of("image/png");
        if (head.length >= HEAD && "RIFF".equals(ascii(head, 0)) && "WEBP".equals(ascii(head, 8))) {
            return Optional.of("image/webp");
        }
        return Optional.empty();
    }

    static String extension(String contentType) {
        return contentType.equals("image/jpeg") ? "jpg" : contentType.substring("image/".length());
    }

    private static boolean startsWith(byte[] head, byte[] magic) {
        return head.length >= magic.length && Arrays.equals(head, 0, magic.length, magic, 0, magic.length);
    }

    private static String ascii(byte[] b, int from) {
        return new String(b, from, 4, StandardCharsets.ISO_8859_1);
    }
}
