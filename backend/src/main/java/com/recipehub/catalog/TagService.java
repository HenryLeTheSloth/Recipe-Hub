package com.recipehub.catalog;

import com.recipehub.catalog.dto.NamedRequest;
import com.recipehub.catalog.dto.TagResponse;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagService {

    private final TagRepository repo;

    public TagService(TagRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> list() {
        return repo.findAll(Sort.by("name")).stream().map(TagService::response).toList();
    }

    @Transactional
    public TagResponse create(NamedRequest r) {
        return write(new Tag(null, null), r);
    }

    @Transactional
    public TagResponse update(Long id, NamedRequest r) {
        return write(repo.findById(id).orElseThrow(Names::notFound), r);
    }

    @Transactional
    public void delete(Long id) {
        Tag t = repo.findById(id).orElseThrow(Names::notFound);
        Names.delete(() -> {
            repo.delete(t);
            repo.flush();
        });
    }

    private TagResponse write(Tag t, NamedRequest r) {
        String name = r.name().trim();
        String base = Names.baseSlug(name);
        Names.requireFree(repo.findByNameIgnoreCase(name), t.getId());
        t.setName(name);
        t.setSlug(Names.uniqueSlug(base, repo::findBySlug, t.getId()));
        return response(Names.save(() -> repo.saveAndFlush(t)));
    }

    static TagResponse response(Tag t) {
        return new TagResponse(t.getId(), t.getName(), t.getSlug());
    }
}
