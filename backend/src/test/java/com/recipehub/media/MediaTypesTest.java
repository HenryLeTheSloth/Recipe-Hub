package com.recipehub.media;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MediaTypesTest {

    static byte[] hex(String h) {
        return HexFormat.of().parseHex(h);
    }

    @Test
    void detectsPng() {
        assertThat(MediaTypes.detect(hex("89504E470D0A1A0A0000"))).contains("image/png");
    }

    @Test
    void detectsJpeg() {
        assertThat(MediaTypes.detect(hex("FFD8FFE000104A464946"))).contains("image/jpeg");
    }

    @Test
    void detectsWebp() {
        assertThat(MediaTypes.detect("RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1))).contains("image/webp");
    }

    @Test
    void rejectsHtmlAndSvg() {
        assertThat(MediaTypes.detect("<html>".getBytes())).isEmpty();
        assertThat(MediaTypes.detect("<svg xmlns=\"x\">".getBytes())).isEmpty();
        assertThat(MediaTypes.detect("RIFF\0\0\0\0WAVEfmt ".getBytes(StandardCharsets.ISO_8859_1))).isEmpty();
    }

    @Test
    void rejectsShortInput() {
        assertThat(MediaTypes.detect(new byte[2])).isEmpty();
        assertThat(MediaTypes.detect(hex("FFD8"))).isEmpty();
    }
}
