package com.recipehub.catalog;

import com.recipehub.catalog.dto.IngredientAdminRequest;
import com.recipehub.catalog.dto.IngredientResponse;
import com.recipehub.catalog.dto.MergeRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ingredients")
@PreAuthorize("hasAuthority('CATALOG_MANAGE')")
public class AdminIngredientController {

    private final IngredientService service;

    public AdminIngredientController(IngredientService service) {
        this.service = service;
    }

    @GetMapping
    public PagedModel<IngredientResponse> list(@RequestParam(required = false) IngredientStatus status,
                                               @RequestParam(required = false) String q, Pageable pageable) {
        // Bỏ sort từ client: query đã có order by cố định.
        return new PagedModel<>(service.adminList(status, q,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())));
    }

    @PutMapping("/{id}")
    public IngredientResponse update(@PathVariable Long id, @Valid @RequestBody IngredientAdminRequest r) {
        return service.update(id, r.ingredient(), r.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/{id}/merge")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void merge(@PathVariable Long id, @Valid @RequestBody MergeRequest r) {
        service.merge(id, r.targetId());
    }
}
