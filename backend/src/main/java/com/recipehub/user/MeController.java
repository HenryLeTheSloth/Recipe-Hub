package com.recipehub.user;

import com.recipehub.user.dto.ChangePasswordRequest;
import com.recipehub.user.dto.MeResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final AuthService auth;

    public MeController(AuthService auth) {
        this.auth = auth;
    }

    @GetMapping
    public MeResponse me(@AuthenticationPrincipal AppUserDetails principal) {
        return auth.me(principal.id());
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AppUserDetails principal,
                               @Valid @RequestBody ChangePasswordRequest r, HttpSession session) {
        auth.changePassword(principal.id(), r.currentPassword(), r.newPassword(), session.getId());
    }
}
