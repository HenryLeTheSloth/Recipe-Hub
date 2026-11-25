package com.recipehub.user;

import com.recipehub.common.ErrorCode;
import com.recipehub.user.dto.RoleRequest;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.recipehub.user.AdminUserServiceTest.assertCode;
import static com.recipehub.user.Fixtures.role;
import static com.recipehub.user.Fixtures.user;
import static com.recipehub.user.Fixtures.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock RoleRepository roles;
    @Mock PermissionRepository permissions;
    @Mock UserRepository users;
    @Mock SessionService sessions;
    RoleService service;

    final Permission read = new Permission("RECIPE_READ", "");
    final Permission write = new Permission("RECIPE_WRITE", "");
    final AppUserDetails full = Fixtures.actor(7, "RECIPE_READ", "RECIPE_WRITE");
    final AppUserDetails readOnly = Fixtures.actor(7, "RECIPE_READ");

    @BeforeEach
    void setUp() {
        service = new RoleService(roles, permissions, users, sessions);
        lenient().when(permissions.findAllByCodeIn(Set.of("RECIPE_READ"))).thenReturn(List.of(read));
        lenient().when(permissions.findAllByCodeIn(Set.of("RECIPE_READ", "RECIPE_WRITE")))
                .thenReturn(List.of(read, write));
        lenient().when(roles.saveAndFlush(any())).thenAnswer(i -> withId(i.getArgument(0), 50L));
    }

    Role given(Role r, boolean system) {
        if (system) {
            org.springframework.test.util.ReflectionTestUtils.setField(r, "system", true);
        }
        lenient().when(roles.findById(r.getId())).thenReturn(Optional.of(r));
        return r;
    }

    @Test
    void adminRolePermissionsAreImmutable() {
        Role admin = given(role(1, "ADMIN", "RECIPE_READ", "RECIPE_WRITE"), true);
        assertCode(() -> service.update(full, 1L, new RoleRequest(null, "Admin", null, Set.of("RECIPE_READ"))),
                ErrorCode.SYSTEM_ROLE);
        assertThat(admin.getPermissions()).hasSize(2);
    }

    @Test
    void adminRoleRenameAllowed() {
        Role admin = given(role(1, "ADMIN", "RECIPE_READ", "RECIPE_WRITE"), true);
        var res = service.update(full, 1L, new RoleRequest(null, "Quản trị", "mô tả", Set.of("RECIPE_READ", "RECIPE_WRITE")));
        assertThat(admin.getName()).isEqualTo("Quản trị");
        assertThat(res.name()).isEqualTo("Quản trị");
        verify(sessions, never()).invalidateAll(any());
    }

    @Test
    void codeIsIgnoredOnUpdate() {
        Role r = given(role(4, "EDITOR", "RECIPE_READ"), false);
        service.update(full, 4L, new RoleRequest("HACKED", "Editor", null, Set.of("RECIPE_READ")));
        assertThat(r.getCode()).isEqualTo("EDITOR");
    }

    @Test
    void systemRoleCannotBeDeleted() {
        given(role(3, "MEMBER"), true);
        assertCode(() -> service.delete(full, 3L), ErrorCode.SYSTEM_ROLE);
    }

    @Test
    void roleInUseCannotBeDeleted() {
        given(role(4, "EDITOR"), false);
        when(users.countByRoles_Id(4L)).thenReturn(2L);
        assertCode(() -> service.delete(full, 4L), ErrorCode.ROLE_IN_USE);
        verify(roles, never()).delete(any());
    }

    @Test
    void unusedCustomRoleIsDeleted() {
        Role r = given(role(4, "EDITOR"), false);
        service.delete(full, 4L);
        verify(roles).delete(r);
    }

    @Test
    void updatingPermissionsInvalidatesMembersSessions() {
        Role r = given(role(4, "EDITOR", "RECIPE_READ"), false);
        when(users.findAllByRoles_Id(4L)).thenReturn(List.of(user(10, "a@x.com", r), user(11, "b@x.com", r)));
        service.update(full, 4L, new RoleRequest(null, "Editor", null, Set.of("RECIPE_READ", "RECIPE_WRITE")));
        assertThat(r.getPermissions()).containsExactlyInAnyOrder(read, write);
        verify(sessions).invalidateAll("a@x.com");
        verify(sessions).invalidateAll("b@x.com");
    }

    @Test
    void duplicateCodeRejected() {
        when(roles.findByCode("EDITOR")).thenReturn(Optional.of(role(4, "EDITOR")));
        assertCode(() -> service.create(full, new RoleRequest("EDITOR", "Editor", null, Set.of("RECIPE_READ"))),
                ErrorCode.ROLE_CODE_TAKEN);
    }

    @Test
    void unknownPermissionIsValidationError() {
        when(permissions.findAllByCodeIn(Set.of("RECIPE_READ", "NOPE"))).thenReturn(List.of(read));
        assertCode(() -> service.create(full, new RoleRequest("EDITOR", "Editor", null, Set.of("RECIPE_READ", "NOPE"))),
                ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void createRequiresCode() {
        assertCode(() -> service.create(full, new RoleRequest(null, "Editor", null, Set.of("RECIPE_READ"))),
                ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void createsCustomRole() {
        var res = service.create(full, new RoleRequest("EDITOR", "Editor", "Biên tập", Set.of("RECIPE_READ")));
        assertThat(res.id()).isEqualTo(50L);
        assertThat(res.system()).isFalse();
        assertThat(res.permissionCodes()).containsExactly("RECIPE_READ");
        assertThat(res.userCount()).isZero();
    }

    @Test
    void cannotCreateRoleWithPermissionActorLacks() {
        assertCode(() -> service.create(readOnly, new RoleRequest("EDITOR", "Editor", null,
                Set.of("RECIPE_READ", "RECIPE_WRITE"))), ErrorCode.FORBIDDEN);
        verify(roles, never()).saveAndFlush(any());
    }

    @Test
    void cannotAddPermissionActorLacks() {
        Role r = given(role(4, "EDITOR", "RECIPE_READ"), false);
        assertCode(() -> service.update(readOnly, 4L, new RoleRequest(null, "Editor", null,
                Set.of("RECIPE_READ", "RECIPE_WRITE"))), ErrorCode.FORBIDDEN);
        assertThat(r.getPermissions()).extracting(Permission::getCode).containsExactly("RECIPE_READ");
    }

    @Test
    void cannotEditOrDeleteRoleAboveActor() {
        given(role(4, "EDITOR", "RECIPE_READ", "RECIPE_WRITE"), false);
        assertCode(() -> service.update(readOnly, 4L, new RoleRequest(null, "Editor", null, Set.of("RECIPE_READ"))),
                ErrorCode.FORBIDDEN);
        assertCode(() -> service.delete(readOnly, 4L), ErrorCode.FORBIDDEN);
        verify(roles, never()).delete(any());
    }

    @Test
    void concurrentDuplicateCodeIsConflictNot500() {
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"roles_code_key\"")).when(roles).saveAndFlush(any());
        assertCode(() -> service.create(full, new RoleRequest("EDITOR", "Editor", null, Set.of("RECIPE_READ"))),
                ErrorCode.ROLE_CODE_TAKEN);
    }
}
