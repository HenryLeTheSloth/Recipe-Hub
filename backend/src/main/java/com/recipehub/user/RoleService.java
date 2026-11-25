package com.recipehub.user;

import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import com.recipehub.user.dto.PermissionResponse;
import com.recipehub.user.dto.RoleRequest;
import com.recipehub.user.dto.RoleResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {

    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final UserRepository users;
    private final SessionService sessions;

    public RoleService(RoleRepository roles, PermissionRepository permissions, UserRepository users,
                       SessionService sessions) {
        this.roles = roles;
        this.permissions = permissions;
        this.users = users;
        this.sessions = sessions;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> list() {
        return roles.findAll(Sort.by("id")).stream()
                .map(r -> RoleResponse.from(r, users.countByRoles_Id(r.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> permissions() {
        return permissions.findAll(Sort.by("code")).stream()
                .map(p -> new PermissionResponse(p.getCode(), p.getDescription())).toList();
    }

    @Transactional
    public RoleResponse create(RoleRequest r) {
        if (r.code() == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Thiếu mã vai trò");
        }
        if (roles.findByCode(r.code()).isPresent()) {
            throw new ApiException(ErrorCode.ROLE_CODE_TAKEN, "Mã vai trò đã tồn tại");
        }
        Role role = new Role(r.code(), r.name().trim(), r.description());
        role.getPermissions().addAll(resolve(r.permissionCodes()));
        return RoleResponse.from(roles.save(role), 0);
    }

    @Transactional
    public RoleResponse update(Long id, RoleRequest r) {
        Role role = find(id);
        Set<Permission> newPerms = resolve(r.permissionCodes());
        boolean permsChanged = !r.permissionCodes().equals(
                role.getPermissions().stream().map(Permission::getCode).collect(Collectors.toSet()));
        if (permsChanged && "ADMIN".equals(role.getCode())) {
            throw new ApiException(ErrorCode.SYSTEM_ROLE, "Không thể đổi quyền của vai trò ADMIN");
        }
        role.setName(r.name().trim());
        role.setDescription(r.description());
        if (permsChanged) {
            role.getPermissions().clear();
            role.getPermissions().addAll(newPerms);
            users.findAllByRoles_Id(id).forEach(u -> sessions.invalidateAll(u.getEmail()));
        }
        return RoleResponse.from(role, users.countByRoles_Id(id));
    }

    @Transactional
    public void delete(Long id) {
        Role role = find(id);
        if (role.isSystem()) {
            throw new ApiException(ErrorCode.SYSTEM_ROLE, "Không thể xóa vai trò hệ thống");
        }
        if (users.countByRoles_Id(id) > 0) {
            throw new ApiException(ErrorCode.ROLE_IN_USE, "Vai trò đang được gán cho người dùng");
        }
        roles.delete(role);
    }

    private Role find(Long id) {
        return roles.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy vai trò"));
    }

    private Set<Permission> resolve(Set<String> codes) {
        Set<Permission> found = new HashSet<>(permissions.findAllByCodeIn(codes));
        if (found.size() != codes.size()) {
            Set<String> known = found.stream().map(Permission::getCode).collect(Collectors.toSet());
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Quyền không tồn tại: "
                    + codes.stream().filter(c -> !known.contains(c)).sorted().toList());
        }
        return found;
    }
}
