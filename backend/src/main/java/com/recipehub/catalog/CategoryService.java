package com.recipehub.catalog;

import com.recipehub.catalog.dto.CategoryRequest;
import com.recipehub.catalog.dto.CategoryResponse;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository repo;

    public CategoryService(CategoryRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return repo.findAll(Sort.by("sortOrder", "name")).stream().map(CategoryService::response).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest r) {
        return write(new Category(null, null, 0), r);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest r) {
        return write(repo.findById(id).orElseThrow(Names::notFound), r);
    }

    @Transactional
    public void delete(Long id) {
        Category c = repo.findById(id).orElseThrow(Names::notFound);
        Names.delete(() -> {
            repo.delete(c);
            repo.flush();
        });
    }

    private CategoryResponse write(Category c, CategoryRequest r) {
        String name = r.name().trim();
        String base = Names.baseSlug(name);
        Names.requireFree(repo.findByNameIgnoreCase(name), c.getId());
        c.setName(name);
        c.setSlug(Names.uniqueSlug(base, repo::findBySlug, c.getId()));
        c.setSortOrder(r.sortOrder());
        return response(Names.save(() -> repo.saveAndFlush(c)));
    }

    static CategoryResponse response(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getSortOrder());
    }
}
