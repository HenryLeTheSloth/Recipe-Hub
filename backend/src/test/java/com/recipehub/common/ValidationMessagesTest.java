package com.recipehub.common;

import jakarta.validation.Validation;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.recipehub.recipe.dto.RecipeRequest;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Ràng buộc không ghi message riêng vẫn báo lỗi tiếng Việt, dù JVM chạy locale nào. */
class ValidationMessagesTest {

    record Form(@NotBlank String name, @NotNull Integer n, @Size(min = 3, max = 120) String title,
                @Size(max = 100) String note, @Min(1) int servings, @Max(50) int max,
                @DecimalMin(value = "0", inclusive = false) BigDecimal qty, @DecimalMax("100000") BigDecimal big,
                @Email String email, @Digits(integer = 6, fraction = 2) BigDecimal digits) {
    }

    @Test
    void defaultMessagesAreVietnamese() {
        Locale before = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Map<String, String> m = factory.getValidator()
                    .validate(new Form(" ", null, "ab", "x".repeat(101), 0, 51, BigDecimal.ZERO, new BigDecimal("100001"), "no", new BigDecimal("1.234")))
                    .stream().collect(Collectors.toMap(v -> v.getPropertyPath().toString(), v -> v.getMessage()));
            assertThat(m).containsEntry("name", "Bắt buộc")
                    .containsEntry("n", "Bắt buộc")
                    .containsEntry("title", "Từ 3 đến 120 ký tự")
                    .containsEntry("note", "Tối đa 100 ký tự")
                    .containsEntry("servings", "Tối thiểu 1")
                    .containsEntry("max", "Tối đa 50")
                    .containsEntry("qty", "Phải lớn hơn 0")
                    .containsEntry("big", "Tối đa 100000")
                    .containsEntry("email", "Email không hợp lệ")
                    .containsEntry("digits", "Tối đa 6 chữ số phần nguyên và 2 chữ số thập phân");
        } finally {
            Locale.setDefault(before);
        }
    }

    @Test
    void listLimitsNameTheirItemsNotCharacters() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var v = factory.getValidator();
            assertThat(v.validateValue(RecipeRequest.class, "ingredients", Collections.nCopies(51, null)))
                    .filteredOn(c -> c.getPropertyPath().toString().equals("ingredients"))
                    .extracting(c -> c.getMessage()).containsExactly("Tối đa 50 nguyên liệu");
            assertThat(v.validateValue(RecipeRequest.class, "steps", Collections.nCopies(31, null)))
                    .filteredOn(c -> c.getPropertyPath().toString().equals("steps"))
                    .extracting(c -> c.getMessage()).containsExactly("Tối đa 30 bước");
        }
    }
}
