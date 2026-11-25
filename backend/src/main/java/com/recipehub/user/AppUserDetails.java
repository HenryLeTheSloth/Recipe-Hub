package com.recipehub.user;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AppUserDetails(Long id, String email, String passwordHash, boolean verified, boolean locked,
                             Set<String> permissions) implements UserDetails, Serializable {

    public static AppUserDetails from(User u) {
        Set<String> perms = u.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toUnmodifiableSet());
        return new AppUserDetails(u.getId(), u.getEmail(), u.getPasswordHash(),
                u.getEmailVerifiedAt() != null, u.isLocked(), perms);
    }

    public boolean can(String permission) {
        return permissions.contains(permission);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissions.stream().map(SimpleGrantedAuthority::new).toList();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isEnabled() {
        return verified;
    }
}
