package com.recipehub.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(@NotBlank @Size(max = 50) String name, Integer sortOrder) {

    /** Ô "Thứ tự" để trống → 0. */
    public int order() {
        return sortOrder == null ? 0 : sortOrder;
    }
}
