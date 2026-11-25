package com.recipehub.catalog;

import com.recipehub.common.ApiException;
import com.recipehub.common.BaseEntity;
import com.recipehub.common.ErrorCode;
import com.recipehub.common.Slugs;
import java.text.Normalizer;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;

/** Luật tên/slug chung cho category, tag, ingredient. */
final class Names {

    private Names() {
    }

    /** Chuẩn hoá tên/từ khoá: NFC (gõ "Unicode tổ hợp" vẫn khớp), gộp khoảng trắng, trim; null → "". */
    static String clean(String s) {
        return s == null ? "" : Normalizer.normalize(s, Normalizer.Form.NFC).replaceAll("\\s+", " ").strip();
    }

    /** Slug gốc từ tên đã trim; rỗng → VALIDATION_ERROR. */
    static String baseSlug(String name) {
        String base = Slugs.slugify(name);
        if (base.isEmpty()) throw new ApiException(ErrorCode.VALIDATION_ERROR, "Tên phải chứa chữ hoặc số");
        return base;
    }

    /** Tên trùng (bỏ qua chính nó) → NAME_TAKEN. */
    static void requireFree(Optional<? extends BaseEntity> found, Long selfId) {
        found.filter(e -> !Objects.equals(e.getId(), selfId)).ifPresent(e -> {
            throw new ApiException(ErrorCode.NAME_TAKEN, "Tên đã tồn tại");
        });
    }

    /** Slug duy nhất, bỏ qua slug của chính nó. */
    static String uniqueSlug(String base, Function<String, Optional<? extends BaseEntity>> bySlug,
                             Long selfId) {
        return Slugs.unique(base, s -> bySlug.apply(s).filter(e -> !Objects.equals(e.getId(), selfId)).isPresent());
    }

    /** Ghi; vi phạm unique do ghi đồng thời → NAME_TAKEN. */
    static <T> T save(Supplier<T> write) {
        try {
            return write.get();
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.NAME_TAKEN, "Tên đã tồn tại");
        }
    }

    static void delete(Runnable delete) {
        try {
            delete.run();
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.IN_USE, "Đang được sử dụng, không thể xóa");
        }
    }

    static ApiException notFound() {
        return new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy");
    }
}
