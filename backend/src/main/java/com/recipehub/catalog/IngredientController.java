package com.recipehub.catalog;

import com.recipehub.catalog.dto.IngredientRequest;
import com.recipehub.catalog.dto.IngredientResponse;
import com.recipehub.user.AppUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingredients")
public class IngredientController {

    private final IngredientService service;

    public IngredientController(IngredientService service) {
        this.service = service;
    }

    @GetMapping
    public List<IngredientResponse> list(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) IngredientGroup group) {
        return service.listApproved(q, group);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('RECIPE_WRITE')")
    public ResponseEntity<IngredientResponse> suggest(@AuthenticationPrincipal AppUserDetails principal,
                                                      @Valid @RequestBody IngredientRequest r) {
        var res = service.suggest(principal.id(), r);
        return ResponseEntity.status(res.created() ? HttpStatus.CREATED : HttpStatus.OK).body(res.ingredient());
    }
}
