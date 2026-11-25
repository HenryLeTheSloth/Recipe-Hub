package com.recipehub.user;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AppUserDetails(Long id, String email, String passwordHash, boolean verified, boolean locked,
                             Set<String> permissions) implements UserDetails, Serializable {

    public static AppUserDetails from(User u) {
        return new AppUserDetails(u.getId(), u.getEmail(), u.getPasswordHash(),
                u.getEmailVerifiedAt() != null, u.isLocked(), u.permissionCodes());
    }

    /** Bản lưu vào session: không mang hash mật khẩu. */
    public AppUserDetails withoutPassword() {
        return new AppUserDetails(id, email, null, verified, locked, permissions);
    }

    /** Thiếu quyền nào trong codes thì FORBIDDEN — không ai cấp/sửa được quyền mình không có. */
    public void requireAll(Set<String> codes) {
        if (!permissions.containsAll(codes)) {
            throw new com.recipehub.common.ApiException(com.recipehub.common.ErrorCode.FORBIDDEN,
                    "Không thể thao tác trên quyền mà bạn không có");
        }
    }

    @Override
    public String toString() {
        return "AppUserDetails[id=" + id + ", email=" + email + ", permissions=" + permissions + "]";
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
