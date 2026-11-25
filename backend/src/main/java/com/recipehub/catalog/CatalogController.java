package com.recipehub.catalog;

import com.recipehub.catalog.dto.CategoryResponse;
import com.recipehub.catalog.dto.TagResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CategoryService categories;
    private final TagService tags;

    public CatalogController(CategoryService categories, TagService tags) {
        this.categories = categories;
        this.tags = tags;
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return categories.list();
    }

    @GetMapping("/tags")
    public List<TagResponse> tags() {
        return tags.list();
    }
}
