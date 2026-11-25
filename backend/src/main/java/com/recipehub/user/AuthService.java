package com.recipehub.user;

import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import com.recipehub.user.dto.RegisterRequest;
import java.time.Clock;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final MailService mail;
    private final Clock clock;

    public AuthService(UserRepository users, RoleRepository roles, PasswordEncoder encoder,
                       TokenService tokens, MailService mail, Clock clock) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
        this.tokens = tokens;
        this.mail = mail;
        this.clock = clock;
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    @Transactional
    public void register(RegisterRequest r) {
        Passwords.validate(r.password());
        String email = normalizeEmail(r.email());
        if (users.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_TAKEN, "Email đã được sử dụng");
        }
        if (users.existsByUsername(r.username())) {
            throw new ApiException(ErrorCode.USERNAME_TAKEN, "Tên đăng nhập đã được sử dụng");
        }
        User user = new User(email, r.username(), encoder.encode(r.password()), r.displayName().trim());
        user.getRoles().add(roles.findByCode("MEMBER").orElseThrow());
        users.save(user);
        mail.sendVerification(email, tokens.issue(user, TokenType.VERIFY_EMAIL));
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = tokens.consume(token, TokenType.VERIFY_EMAIL);
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(clock.instant());
        }
    }

    @Transactional
    public void resendVerification(String email) {
        users.findByEmail(normalizeEmail(email))
                .filter(u -> u.getEmailVerifiedAt() == null && !u.isLocked())
                .ifPresent(u -> {
                    tokens.invalidateUnused(u, TokenType.VERIFY_EMAIL);
                    mail.sendVerification(u.getEmail(), tokens.issue(u, TokenType.VERIFY_EMAIL));
                });
    }
}
