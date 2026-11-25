package com.recipehub.media;

import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class LocalStorageService implements StorageService {

    private final Path dir;

    public LocalStorageService(@Value("${app.storage.dir}") String dir) {
        this.dir = Path.of(dir).toAbsolutePath().normalize();
    }

    @Override
    public String store(InputStream in, String extension) {
        String key = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(dir);
            Files.copy(in, dir.resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return key;
    }

    @Override
    public Resource load(String key) {
        Path p = resolve(key);
        if (!Files.isRegularFile(p)) {
            throw notFound();
        }
        return new FileSystemResource(p);
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path resolve(String key) {
        if (key == null || key.contains("/") || key.contains("\\") || key.contains("..")) {
            throw notFound();
        }
        return dir.resolve(key);
    }

    private static ApiException notFound() {
        return new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy ảnh");
    }
}
