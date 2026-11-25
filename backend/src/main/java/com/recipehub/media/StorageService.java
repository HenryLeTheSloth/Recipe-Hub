package com.recipehub.media;

import java.io.InputStream;
import org.springframework.core.io.Resource;

/** Nơi giữ file ảnh; bản local hiện tại, có thể thay bằng MinIO/S3. */
public interface StorageService {

    String store(InputStream in, String extension);

    Resource load(String key);

    void delete(String key);
}
