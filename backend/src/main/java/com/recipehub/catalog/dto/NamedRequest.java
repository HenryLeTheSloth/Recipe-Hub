package com.recipehub.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NamedRequest(@NotBlank @Size(max = 50) String name) {
}
