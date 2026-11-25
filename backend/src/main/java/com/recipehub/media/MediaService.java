package com.recipehub.media;

import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import com.recipehub.media.dto.MediaResponse;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaService {

    static final long MAX_BYTES = 5L * 1024 * 1024;

    private final MediaRepository media;
    private final StorageService storage;

    public MediaService(MediaRepository media, StorageService storage) {
        this.media = media;
        this.storage = storage;
    }

    // ponytail: file đã ghi mà transaction rollback (hoặc media bị xóa theo user) thì thành file mồ côi;
    // thêm job dọn khi dung lượng uploads/ đáng kể.
    @Transactional
    public MediaResponse upload(Long ownerId, MultipartFile file) {
        if (file.getSize() > MAX_BYTES) {
            throw new ApiException(ErrorCode.MEDIA_TOO_LARGE, "Ảnh tối đa 5MB");
        }
        try (InputStream in = new BufferedInputStream(file.getInputStream())) {
            in.mark(MediaTypes.HEAD);
            String type = MediaTypes.detect(in.readNBytes(MediaTypes.HEAD))
                    .orElseThrow(() -> new ApiException(ErrorCode.MEDIA_TYPE, "Chỉ nhận ảnh JPEG, PNG hoặc WEBP"));
            in.reset();
            String key = storage.store(in, MediaTypes.extension(type));
            return response(media.save(new Media(ownerId, key, type, file.getSize())));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Transactional(readOnly = true)
    public Media get(Long id) {
        return media.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy ảnh"));
    }

    @Transactional(readOnly = true)
    public Media requireOwned(Long mediaId, Long userId) {
        Media m = get(mediaId);
        if (!m.getOwnerId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Ảnh không thuộc về bạn");
        }
        return m;
    }

    public static String url(Media m) {
        return m == null ? null : "/api/media/" + m.getId() + "/file";
    }

    static MediaResponse response(Media m) {
        return new MediaResponse(m.getId(), url(m));
    }
}
