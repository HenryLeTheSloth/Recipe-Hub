package com.recipehub.media;

import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalStorageServiceTest {

    @TempDir Path dir;

    @Test
    void storeThenLoadRoundTrips() throws Exception {
        var storage = new LocalStorageService(dir.resolve("uploads").toString());
        String key = storage.store(new ByteArrayInputStream(new byte[]{1, 2, 3}), "png");
        assertThat(key).matches("[0-9a-f-]{36}\\.png");
        assertThat(storage.load(key).getContentAsByteArray()).containsExactly(1, 2, 3);
        storage.delete(key);
        assertThatThrownBy(() -> storage.load(key)).extracting(e -> ((ApiException) e).code()).isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void rejectsPathTraversal() {
        var storage = new LocalStorageService(dir.toString());
        for (String key : new String[]{"../secret", "a/b.png", "a\\b.png", ".."}) {
            assertThatThrownBy(() -> storage.load(key)).isInstanceOf(ApiException.class)
                    .extracting(e -> ((ApiException) e).code()).isEqualTo(ErrorCode.NOT_FOUND);
        }
    }
}
