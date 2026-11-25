package com.recipehub.catalog.dto;

import jakarta.validation.constraints.NotNull;

public record MergeRequest(@NotNull Long targetId) {
}
