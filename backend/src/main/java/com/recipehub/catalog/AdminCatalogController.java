package com.recipehub.catalog;

import com.recipehub.catalog.dto.CategoryRequest;
import com.recipehub.catalog.dto.CategoryResponse;
import com.recipehub.catalog.dto.NamedRequest;
import com.recipehub.catalog.dto.TagResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('CATALOG_MANAGE')")
public class AdminCatalogController {

    private final CategoryService categories;
    private final TagService tags;

    public AdminCatalogController(CategoryService categories, TagService tags) {
        this.categories = categories;
        this.tags = tags;
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest r) {
        return categories.create(r);
    }

    @PutMapping("/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest r) {
        return categories.update(id, r);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        categories.delete(id);
    }

    @PostMapping("/tags")
    @ResponseStatus(HttpStatus.CREATED)
    public TagResponse createTag(@Valid @RequestBody NamedRequest r) {
        return tags.create(r);
    }

    @PutMapping("/tags/{id}")
    public TagResponse updateTag(@PathVariable Long id, @Valid @RequestBody NamedRequest r) {
        return tags.update(id, r);
    }

    @DeleteMapping("/tags/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTag(@PathVariable Long id) {
        tags.delete(id);
    }
}
