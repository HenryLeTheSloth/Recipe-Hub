package com.recipehub.user;

import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import com.recipehub.user.dto.MeResponse;
import com.recipehub.user.dto.RegisterRequest;
import java.time.Clock;
import java.util.List;
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
    private final SessionService sessions;
    private final Clock clock;

    public AuthService(UserRepository users, RoleRepository roles, PasswordEncoder encoder,
                       TokenService tokens, MailService mail, SessionService sessions, Clock clock) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
        this.tokens = tokens;
        this.mail = mail;
        this.sessions = sessions;
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

    @Transactional
    public void forgotPassword(String email) {
        users.findByEmail(normalizeEmail(email))
                .filter(u -> !u.isLocked())
                .ifPresent(u -> {
                    tokens.invalidateUnused(u, TokenType.RESET_PASSWORD);
                    mail.sendPasswordReset(u.getEmail(), tokens.issue(u, TokenType.RESET_PASSWORD));
                });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        Passwords.validate(newPassword);
        User user = tokens.consume(token, TokenType.RESET_PASSWORD);
        user.setPasswordHash(encoder.encode(newPassword));
        sessions.invalidateAll(user.getEmail());
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword, String currentSessionId) {
        User user = users.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED, "Chưa đăng nhập"));
        if (!encoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ApiException(ErrorCode.WRONG_PASSWORD, "Mật khẩu hiện tại không đúng");
        }
        Passwords.validate(newPassword);
        user.setPasswordHash(encoder.encode(newPassword));
        sessions.invalidateOthers(user.getEmail(), currentSessionId);
    }

    @Transactional(readOnly = true)
    public MeResponse me(Long userId) {
        User u = users.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED, "Chưa đăng nhập"));
        List<String> roleCodes = u.getRoles().stream().map(Role::getCode).sorted().toList();
        List<String> perms = u.getRoles().stream().flatMap(r -> r.getPermissions().stream())
                .map(Permission::getCode).distinct().sorted().toList();
        return new MeResponse(u.getId(), u.getEmail(), u.getUsername(), u.getDisplayName(), null, roleCodes, perms);
    }
}
