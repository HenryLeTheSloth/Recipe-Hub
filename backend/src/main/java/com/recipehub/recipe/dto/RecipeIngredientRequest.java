package com.recipehub.recipe.dto;

import com.recipehub.catalog.Unit;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Một dòng nguyên liệu; position = chỉ số trong list. @Digits khớp numeric(10,2) để 0.001 không bị làm tròn thành 0. */
public record RecipeIngredientRequest(
        @NotNull Long ingredientId,
        @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("100000") @Digits(integer = 6, fraction = 2)
        BigDecimal quantity,
        @NotNull Unit unit,
        @Size(max = 100) String note) {
}
