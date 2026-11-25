package com.recipehub.user;

import com.recipehub.common.ErrorCode;
import com.recipehub.user.dto.RegisterRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    static final Instant NOW = Instant.parse("2026-11-25T01:00:00Z");

    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock PasswordEncoder encoder;
    @Mock TokenService tokens;
    @Mock MailService mail;
    AuthService service;
    final Role member = new Role("MEMBER", "Thành viên", null);

    @BeforeEach
    void setUp() {
        service = new AuthService(users, roles, encoder, tokens, mail, Clock.fixed(NOW, ZoneOffset.UTC));
        lenient().when(roles.findByCode("MEMBER")).thenReturn(Optional.of(member));
        lenient().when(encoder.encode(any())).thenReturn("HASH");
        lenient().when(users.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(tokens.issue(any(), any())).thenReturn("RAW");
    }

    RegisterRequest req(String email) {
        return new RegisterRequest(email, "foo", "Foo", "password1");
    }

    User registered() {
        var captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void registerNormalizesEmail() {
        service.register(req("  Foo@X.com "));
        assertThat(registered().getEmail()).isEqualTo("foo@x.com");
    }

    @Test
    void registerRejectsTakenEmail() {
        when(users.existsByEmail("foo@x.com")).thenReturn(true);
        assertThatThrownBy(() -> service.register(req("Foo@x.com")))
                .hasFieldOrPropertyWithValue("code", ErrorCode.EMAIL_TAKEN);
    }

    @Test
    void registerRejectsTakenUsername() {
        when(users.existsByUsername("foo")).thenReturn(true);
        assertThatThrownBy(() -> service.register(req("foo@x.com")))
                .hasFieldOrPropertyWithValue("code", ErrorCode.USERNAME_TAKEN);
    }

    @Test
    void registerRejectsWeakPassword() {
        assertThatThrownBy(() -> service.register(new RegisterRequest("foo@x.com", "foo", "Foo", "short")))
                .hasFieldOrPropertyWithValue("code", ErrorCode.INVALID_PASSWORD);
    }

    @Test
    void registerAssignsMemberAndHashesPassword() {
        service.register(req("foo@x.com"));
        User u = registered();
        assertThat(u.getRoles()).containsExactly(member);
        assertThat(u.getPasswordHash()).isEqualTo("HASH");
        assertThat(u.getEmailVerifiedAt()).isNull();
        verify(encoder).encode("password1");
    }

    @Test
    void registerSendsVerificationMail() {
        service.register(req("foo@x.com"));
        verify(tokens).issue(any(User.class), eq(TokenType.VERIFY_EMAIL));
        verify(mail).sendVerification("foo@x.com", "RAW");
    }

    @Test
    void resendIgnoresUnknownEmail() {
        when(users.findByEmail("nobody@x.com")).thenReturn(Optional.empty());
        service.resendVerification(" Nobody@x.com");
        verifyNoInteractions(mail);
    }

    @Test
    void resendIgnoresVerifiedUser() {
        User u = new User("v@x.com", "v", "h", "V");
        u.setEmailVerifiedAt(NOW);
        when(users.findByEmail("v@x.com")).thenReturn(Optional.of(u));
        service.resendVerification("v@x.com");
        verifyNoInteractions(mail);
    }

    @Test
    void resendSendsForUnverifiedUser() {
        User u = new User("n@x.com", "n", "h", "N");
        when(users.findByEmail("n@x.com")).thenReturn(Optional.of(u));
        service.resendVerification("n@x.com");
        verify(tokens).invalidateUnused(u, TokenType.VERIFY_EMAIL);
        verify(mail).sendVerification("n@x.com", "RAW");
    }

    @Test
    void verifySetsVerifiedAt() {
        User u = new User("n@x.com", "n", "h", "N");
        when(tokens.consume("tok", TokenType.VERIFY_EMAIL)).thenReturn(u);
        service.verifyEmail("tok");
        assertThat(u.getEmailVerifiedAt()).isEqualTo(NOW);
    }
}
