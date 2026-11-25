package com.recipehub.user;

import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static com.recipehub.user.Fixtures.actor;
import static com.recipehub.user.Fixtures.role;
import static com.recipehub.user.Fixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock SessionService sessions;
    AdminUserService service;

    final Role admin = role(1, "ADMIN");
    final Role member = role(3, "MEMBER");
    final Role moderator = role(2, "MODERATOR");

    @BeforeEach
    void setUp() {
        service = new AdminUserService(users, roles, sessions);
    }

    void given(User u) {
        lenient().when(users.findById(u.getId())).thenReturn(Optional.of(u));
    }

    static void assertCode(ThrowingCallable call, ErrorCode code) {
        assertThatThrownBy(call).isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code()).isEqualTo(code);
    }

    @Test
    void cannotLockSelf() {
        given(user(7, "me@x.com", admin));
        assertCode(() -> service.setLocked(actor(7), 7L, true), ErrorCode.SELF_ACTION);
    }

    @Test
    void cannotChangeOwnRolesOrDeleteSelf() {
        given(user(7, "me@x.com", admin));
        assertCode(() -> service.setRoles(actor(7), 7L, Set.of("MEMBER")), ErrorCode.SELF_ACTION);
        assertCode(() -> service.delete(actor(7), 7L), ErrorCode.SELF_ACTION);
    }

    @Test
    void unknownUserIsNotFound() {
        when(users.findById(99L)).thenReturn(Optional.empty());
        assertCode(() -> service.setLocked(actor(1), 99L, true), ErrorCode.NOT_FOUND);
    }

    @Test
    void cannotRemoveAdminFromLastAdmin() {
        given(user(5, "a@x.com", admin));
        when(roles.findAllByCodeIn(Set.of("MEMBER"))).thenReturn(List.of(member));
        when(users.countByRoles_CodeAndLockedFalse("ADMIN")).thenReturn(1L);
        assertCode(() -> service.setRoles(actor(1), 5L, Set.of("MEMBER")), ErrorCode.LAST_ADMIN);
    }

    @Test
    void cannotLockLastAdmin() {
        given(user(5, "a@x.com", admin));
        when(users.countByRoles_CodeAndLockedFalse("ADMIN")).thenReturn(1L);
        assertCode(() -> service.setLocked(actor(1), 5L, true), ErrorCode.LAST_ADMIN);
    }

    @Test
    void cannotDeleteLastAdmin() {
        given(user(5, "a@x.com", admin));
        when(users.countByRoles_CodeAndLockedFalse("ADMIN")).thenReturn(1L);
        assertCode(() -> service.delete(actor(1), 5L), ErrorCode.LAST_ADMIN);
    }

    @Test
    void canLockAdminWhenAnotherAdminRemains() {
        User target = user(5, "a@x.com", admin);
        given(target);
        when(users.countByRoles_CodeAndLockedFalse("ADMIN")).thenReturn(2L);
        assertThat(service.setLocked(actor(1), 5L, true).locked()).isTrue();
    }

    @Test
    void lockInvalidatesSessions() {
        User target = user(5, "u@x.com", member);
        given(target);
        var res = service.setLocked(actor(1), 5L, true);
        assertThat(target.isLocked()).isTrue();
        assertThat(res.locked()).isTrue();
        verify(sessions).invalidateAll("u@x.com");
    }

    @Test
    void unlockDoesNotTouchSessions() {
        User target = user(5, "u@x.com", member);
        target.setLocked(true);
        given(target);
        service.setLocked(actor(1), 5L, false);
        assertThat(target.isLocked()).isFalse();
        verify(sessions, never()).invalidateAll(any());
    }

    @Test
    void changingRolesInvalidatesSessions() {
        User target = user(5, "u@x.com", member);
        given(target);
        when(roles.findAllByCodeIn(Set.of("MODERATOR", "MEMBER"))).thenReturn(List.of(moderator, member));
        var res = service.setRoles(actor(1), 5L, Set.of("MODERATOR", "MEMBER"));
        assertThat(target.getRoles()).containsExactlyInAnyOrder(moderator, member);
        assertThat(res.roles()).containsExactly("MEMBER", "MODERATOR");
        verify(sessions).invalidateAll("u@x.com");
    }

    @Test
    void unknownRoleCodeIsNotFound() {
        given(user(5, "u@x.com", member));
        when(roles.findAllByCodeIn(Set.of("MEMBER", "NOPE"))).thenReturn(List.of(member));
        assertCode(() -> service.setRoles(actor(1), 5L, Set.of("MEMBER", "NOPE")), ErrorCode.NOT_FOUND);
        verify(sessions, never()).invalidateAll(any());
    }

    @Test
    void deleteRemovesUserAndSessions() {
        User target = user(5, "u@x.com", member);
        given(target);
        service.delete(actor(1), 5L);
        verify(users).delete(target);
        verify(users).flush();
        verify(sessions).invalidateAll("u@x.com");
    }

    @Test
    void deleteWithContentIs409() {
        given(user(5, "u@x.com", member));
        doThrow(new DataIntegrityViolationException("fk")).when(users).flush();
        assertCode(() -> service.delete(actor(1), 5L), ErrorCode.USER_HAS_CONTENT);
        verify(sessions, never()).invalidateAll(any());
    }
}
