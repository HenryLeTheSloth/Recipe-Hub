package com.recipehub.catalog;

import com.recipehub.catalog.dto.IngredientRequest;
import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.test.util.ReflectionTestUtils;

import static com.recipehub.catalog.IngredientGroup.SAUCE;
import static com.recipehub.catalog.IngredientGroup.VEGETABLE;
import static com.recipehub.catalog.IngredientStatus.APPROVED;
import static com.recipehub.catalog.IngredientStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngredientServiceTest {

    @Mock IngredientRepository repo;
    IngredientService service;

    @BeforeEach
    void setUp() {
        service = new IngredientService(repo);
        lenient().when(repo.findBySlug(any())).thenReturn(Optional.empty());
        lenient().when(repo.findByNameIgnoreCase(any())).thenReturn(Optional.empty());
        lenient().when(repo.saveAndFlush(any())).thenAnswer(i -> {
            Ingredient x = i.getArgument(0);
            if (x.getId() == null) ReflectionTestUtils.setField(x, "id", 30L);
            return x;
        });
    }

    static Ingredient tomato() {
        Ingredient i = new Ingredient("Cà chua", "ca-chua", VEGETABLE, "🍅", APPROVED, null);
        ReflectionTestUtils.setField(i, "id", 1L);
        return i;
    }

    static void assertCode(ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call).isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo(code);
    }

    @Test
    void suggestReturnsExistingIgnoringCaseAndSpaces() {
        when(repo.findByNameIgnoreCase("cà chua")).thenReturn(Optional.of(tomato()));
        var res = service.suggest(7L, new IngredientRequest("  cà chua ", VEGETABLE, null));
        assertThat(res.created()).isFalse();
        assertThat(res.ingredient().id()).isEqualTo(1L);
        verify(repo, never()).saveAndFlush(any());
    }

    @Test
    void suggestWithSlugCollisionCreatesWithCounter() {
        // "Dừa" và "Dứa" cùng slug "dua" nhưng là hai nguyên liệu khác nhau → spec: thêm -2
        Ingredient pineapple = new Ingredient("Dứa", "dua", IngredientGroup.FRUIT, "🍍", APPROVED, null);
        ReflectionTestUtils.setField(pineapple, "id", 2L);
        when(repo.findBySlug("dua")).thenReturn(Optional.of(pineapple));
        var res = service.suggest(7L, new IngredientRequest("Dừa", IngredientGroup.FRUIT, "🥥"));
        assertThat(res.created()).isTrue();
        assertThat(res.ingredient().name()).isEqualTo("Dừa");
        assertThat(res.ingredient().slug()).isEqualTo("dua-2");
    }

    @Test
    void suggestCreatesPending() {
        var res = service.suggest(7L, new IngredientRequest("Lá mơ", VEGETABLE, "🌿"));
        assertThat(res.created()).isTrue();
        assertThat(res.ingredient().status()).isEqualTo(PENDING);
        assertThat(res.ingredient().slug()).isEqualTo("la-mo");
        var saved = ArgumentCaptor.forClass(Ingredient.class);
        verify(repo).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getCreatedBy()).isEqualTo(7L);
    }

    @Test
    void symbolOnlyNameRejected() {
        assertCode(() -> service.suggest(7L, new IngredientRequest("???", VEGETABLE, null)), ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void listApprovedSearchesBySlugCappedAt200() {
        when(repo.search("ca chua", "ca-chua", null, APPROVED, Limit.of(200))).thenReturn(List.of(tomato()));
        assertThat(service.listApproved("ca chua", null)).extracting(r -> r.slug()).containsExactly("ca-chua");
        when(repo.search("", "", SAUCE, APPROVED, Limit.of(200))).thenReturn(List.of());
        assertThat(service.listApproved(null, SAUCE)).isEmpty();
    }

    @Test
    void updateApprovesAndRenames() {
        Ingredient i = new Ingredient("Lá mơ", "la-mo", VEGETABLE, null, PENDING, 7L);
        ReflectionTestUtils.setField(i, "id", 9L);
        when(repo.findById(9L)).thenReturn(Optional.of(i));
        var res = service.update(9L, new IngredientRequest("Lá mơ lông", VEGETABLE, "🌿"), APPROVED);
        assertThat(res.status()).isEqualTo(APPROVED);
        assertThat(res.slug()).isEqualTo("la-mo-long");
        assertThat(res.emoji()).isEqualTo("🌿");
    }

    @Test
    void updateToOtherIngredientsNameIsTaken() {
        Ingredient i = new Ingredient("Lá mơ", "la-mo", VEGETABLE, null, PENDING, 7L);
        ReflectionTestUtils.setField(i, "id", 9L);
        when(repo.findById(9L)).thenReturn(Optional.of(i));
        when(repo.findByNameIgnoreCase("Cà chua")).thenReturn(Optional.of(tomato()));
        assertCode(() -> service.update(9L, new IngredientRequest("Cà chua", VEGETABLE, null), APPROVED),
                ErrorCode.NAME_TAKEN);
    }

    @Test
    void deleteBlockedByFkIsInUse() {
        when(repo.findById(1L)).thenReturn(Optional.of(tomato()));
        doThrow(new DataIntegrityViolationException("fk")).when(repo).flush();
        assertCode(() -> service.delete(1L), ErrorCode.IN_USE);
    }
}
