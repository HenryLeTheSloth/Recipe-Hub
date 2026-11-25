package com.recipehub.catalog;

import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import com.recipehub.catalog.dto.CategoryRequest;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock CategoryRepository repo;
    CategoryService service;

    @BeforeEach
    void setUp() {
        service = new CategoryService(repo);
        lenient().when(repo.findBySlug(any())).thenReturn(Optional.empty());
        lenient().when(repo.saveAndFlush(any())).thenAnswer(i -> {
            Category c = i.getArgument(0);
            if (c.getId() == null) ReflectionTestUtils.setField(c, "id", 20L);
            return c;
        });
    }

    static Category category(long id, String name, String slug) {
        Category c = new Category(name, slug, 0);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    static void assertCode(ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call).isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo(code);
    }

    @Test
    void createsWithSlugAndTrimmedName() {
        var res = service.create(new CategoryRequest("  Món chính ", 3));
        assertThat(res.name()).isEqualTo("Món chính");
        assertThat(res.slug()).isEqualTo("mon-chinh");
        assertThat(res.sortOrder()).isEqualTo(3);
    }

    @Test
    void duplicateNameIgnoringCaseIsTaken() {
        when(repo.findByNameIgnoreCase("món chính")).thenReturn(Optional.of(category(1, "Món chính", "mon-chinh")));
        assertCode(() -> service.create(new CategoryRequest("món chính", 0)), ErrorCode.NAME_TAKEN);
        verify(repo, never()).saveAndFlush(any());
    }

    @Test
    void sameSlugDifferentNameGetsCounter() {
        when(repo.findBySlug("mon-chinh")).thenReturn(Optional.of(category(1, "Món chính", "mon-chinh")));
        assertThat(service.create(new CategoryRequest("Mon chinh", 0)).slug()).isEqualTo("mon-chinh-2");
    }

    @Test
    void renameRegeneratesSlug() {
        Category c = category(5, "Món chính", "mon-chinh");
        when(repo.findById(5L)).thenReturn(Optional.of(c));
        var res = service.update(5L, new CategoryRequest("Món phụ", 2));
        assertThat(res.slug()).isEqualTo("mon-phu");
        assertThat(c.getName()).isEqualTo("Món phụ");
        assertThat(c.getSortOrder()).isEqualTo(2);
    }

    @Test
    void renameToOwnNameKeepsSlug() {
        Category c = category(5, "Món chính", "mon-chinh");
        when(repo.findById(5L)).thenReturn(Optional.of(c));
        when(repo.findByNameIgnoreCase("MÓN CHÍNH")).thenReturn(Optional.of(c));
        when(repo.findBySlug("mon-chinh")).thenReturn(Optional.of(c));
        assertThat(service.update(5L, new CategoryRequest("MÓN CHÍNH", 0)).slug()).isEqualTo("mon-chinh");
    }

    @Test
    void symbolOnlyNameRejected() {
        assertCode(() -> service.create(new CategoryRequest("!!!", 0)), ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void concurrentDuplicateIsNameTaken() {
        doThrow(new DataIntegrityViolationException("dup")).when(repo).saveAndFlush(any());
        assertCode(() -> service.create(new CategoryRequest("Món chính", 0)), ErrorCode.NAME_TAKEN);
    }

    @Test
    void deleteBlockedByFkIsInUse() {
        when(repo.findById(5L)).thenReturn(Optional.of(category(5, "A", "a")));
        doThrow(new DataIntegrityViolationException("fk")).when(repo).flush();
        assertCode(() -> service.delete(5L), ErrorCode.IN_USE);
    }

    @Test
    void unknownIsNotFound() {
        when(repo.findById(9L)).thenReturn(Optional.empty());
        assertCode(() -> service.delete(9L), ErrorCode.NOT_FOUND);
    }
}
