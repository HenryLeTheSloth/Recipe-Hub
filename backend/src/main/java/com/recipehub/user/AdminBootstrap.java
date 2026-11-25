package com.recipehub.user;

import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Tạo admin đầu tiên từ ADMIN_EMAIL/ADMIN_PASSWORD khi hệ thống chưa có admin nào. */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final String email;
    private final String password;

    public AdminBootstrap(UserRepository users, RoleRepository roles, PasswordEncoder encoder, Clock clock,
                          @Value("${app.admin.email:}") String email, @Value("${app.admin.password:}") String password) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
        this.clock = clock;
        this.email = AuthService.normalizeEmail(email);
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.countByRoles_Code("ADMIN") > 0) {
            return;
        }
        if (email.isEmpty() || password == null || password.isBlank()) {
            log.warn("Chưa có admin và ADMIN_EMAIL/ADMIN_PASSWORD trống — bỏ qua tạo admin");
            return;
        }
        if (users.findByEmail(email).isPresent()) {
            log.warn("Email {} đã được đăng ký nhưng không phải admin — bỏ qua tạo admin", email);
            return;
        }
        Passwords.validate(password);
        String username = "admin";
        for (int i = 1; users.existsByUsernameIgnoreCase(username); i++) {
            username = "admin" + i;
        }
        User admin = new User(email, username, encoder.encode(password), "Quản trị viên");
        admin.setEmailVerifiedAt(clock.instant());
        admin.getRoles().add(roles.findByCode("ADMIN").orElseThrow());
        users.save(admin);
        log.info("Đã tạo tài khoản admin {}", email);
    }
}
