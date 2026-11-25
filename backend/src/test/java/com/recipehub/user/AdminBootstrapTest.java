package com.recipehub.user;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static com.recipehub.user.Fixtures.role;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    static final Instant NOW = Instant.parse("2026-11-25T00:00:00Z");

    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock PasswordEncoder encoder;

    AdminBootstrap bootstrap(String email, String password) {
        return new AdminBootstrap(users, roles, encoder, Clock.fixed(NOW, ZoneOffset.UTC), email, password);
    }

    @Test
    void createsAdminWhenNoneAndEnvSet() throws Exception {
        Role admin = role(1, "ADMIN");
        when(users.countByRoles_Code("ADMIN")).thenReturn(0L);
        when(users.findByEmail("admin@x.com")).thenReturn(Optional.empty());
        when(roles.findByCode("ADMIN")).thenReturn(Optional.of(admin));
        when(encoder.encode("ChangeMe123!")).thenReturn("HASH");

        bootstrap(" Admin@X.com ", "ChangeMe123!").run(null);

        var saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        User u = saved.getValue();
        assertThat(u.getEmail()).isEqualTo("admin@x.com");
        assertThat(u.getUsername()).isEqualTo("admin");
        assertThat(u.getPasswordHash()).isEqualTo("HASH");
        assertThat(u.getEmailVerifiedAt()).isEqualTo(NOW);
        assertThat(u.getRoles()).containsExactly(admin);
    }

    @Test
    void idempotentWhenAdminExists() throws Exception {
        when(users.countByRoles_Code("ADMIN")).thenReturn(1L);
        bootstrap("admin@x.com", "ChangeMe123!").run(null);
        verify(users, never()).save(any());
    }

    @Test
    void skipsWhenEnvBlank() throws Exception {
        when(users.countByRoles_Code("ADMIN")).thenReturn(0L);
        bootstrap("", " ").run(null);
        verify(users, never()).save(any());
    }

    @Test
    void skipsWhenEmailAlreadyRegistered() throws Exception {
        when(users.countByRoles_Code("ADMIN")).thenReturn(0L);
        when(users.findByEmail("admin@x.com")).thenReturn(Optional.of(Fixtures.user(9, "admin@x.com")));
        bootstrap("admin@x.com", "ChangeMe123!").run(null);
        verify(users, never()).save(any());
    }

    @Test
    void picksFreeUsernameWhenAdminTaken() throws Exception {
        when(users.countByRoles_Code("ADMIN")).thenReturn(0L);
        when(users.findByEmail("admin@x.com")).thenReturn(Optional.empty());
        when(users.existsByUsernameIgnoreCase("admin")).thenReturn(true);
        when(users.existsByUsernameIgnoreCase("admin1")).thenReturn(true);
        when(roles.findByCode("ADMIN")).thenReturn(Optional.of(role(1, "ADMIN")));
        bootstrap("admin@x.com", "ChangeMe123!").run(null);
        var saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("admin2");
    }
}
