package com.recipehub.user;

import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import com.recipehub.user.dto.AdminUserResponse;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final SessionService sessions;

    public AdminUserService(UserRepository users, RoleRepository roles, SessionService sessions) {
        this.users = users;
        this.roles = roles;
        this.sessions = sessions;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> list(String q, Pageable pageable) {
        // ponytail: roles nạp lazy mỗi dòng (N+1, tối đa 50 dòng/trang); dùng @EntityGraph nếu chậm
        return users.search(q == null ? "" : q.trim(), pageable).map(AdminUserResponse::from);
    }

    @Transactional
    public AdminUserResponse setLocked(AppUserDetails actor, Long id, boolean locked) {
        roles.lockAdminRole();
        User target = target(actor, id);
        if (locked) {
            guardLastAdmin(target);
        }
        target.setLocked(locked);
        if (locked) {
            sessions.invalidateAll(target.getEmail());
        }
        return AdminUserResponse.from(target);
    }

    @Transactional
    public AdminUserResponse setRoles(AppUserDetails actor, Long id, Set<String> roleCodes) {
        roles.lockAdminRole();
        User target = target(actor, id);
        List<Role> newRoles = roles.findAllByCodeIn(roleCodes);
        if (newRoles.size() != roleCodes.size()) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Vai trò không tồn tại");
        }
        newRoles.forEach(r -> actor.requireAll(codes(r)));
        if (!roleCodes.contains("ADMIN")) {
            guardLastAdmin(target);
        }
        target.getRoles().clear();
        target.getRoles().addAll(newRoles);
        sessions.invalidateAll(target.getEmail());
        return AdminUserResponse.from(target);
    }

    @Transactional
    public void delete(AppUserDetails actor, Long id) {
        roles.lockAdminRole();
        User target = target(actor, id);
        guardLastAdmin(target);
        try {
            users.delete(target);
            users.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.USER_HAS_CONTENT, "Người dùng còn nội dung, hãy khóa thay vì xóa");
        }
        sessions.invalidateAll(target.getEmail());
    }

    private User target(AppUserDetails actor, Long id) {
        User target = users.findById(id).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Không tìm thấy người dùng"));
        if (target.getId().equals(actor.id())) {
            throw new ApiException(ErrorCode.SELF_ACTION, "Không thể thao tác trên chính tài khoản của bạn");
        }
        actor.requireAll(target.permissionCodes());
        return target;
    }

    static Set<String> codes(Role r) {
        return r.getPermissions().stream().map(Permission::getCode).collect(java.util.stream.Collectors.toSet());
    }

    /** Không để hệ thống mất admin cuối cùng còn dùng được (chưa bị khóa). */
    private void guardLastAdmin(User target) {
        if (target.hasRole("ADMIN") && !target.isLocked() && users.countByRoles_CodeAndLockedFalse("ADMIN") <= 1) {
            throw new ApiException(ErrorCode.LAST_ADMIN, "Không thể bỏ quản trị viên cuối cùng");
        }
    }
}
