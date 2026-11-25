package com.recipehub.user;

import com.recipehub.common.ApiException;
import com.recipehub.common.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserTokenRepository tokens;
    private final Clock clock;

    public TokenService(UserTokenRepository tokens, Clock clock) {
        this.tokens = tokens;
        this.clock = clock;
    }

    @Transactional
    public String issue(User user, TokenType type) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Duration ttl = type == TokenType.VERIFY_EMAIL ? Duration.ofHours(24) : Duration.ofMinutes(30);
        tokens.save(new UserToken(user, type, sha256Hex(raw), clock.instant().plus(ttl)));
        return raw;
    }

    @Transactional
    public User consume(String raw, TokenType type) {
        Instant now = clock.instant();
        UserToken t = raw == null ? null : tokens.findByTokenHash(sha256Hex(raw))
                .filter(x -> x.getType() == type && x.getUsedAt() == null && x.getExpiresAt().isAfter(now))
                .orElse(null);
        if (t == null || tokens.markUsed(t.getId(), now) == 0) { // markUsed nguyên tử: chặn dùng 2 lần song song
            throw new ApiException(ErrorCode.INVALID_TOKEN, "Liên kết không hợp lệ hoặc đã hết hạn");
        }
        t.setUsedAt(now);
        return t.getUser();
    }

    @Transactional
    public void invalidateUnused(User user, TokenType type) {
        tokens.invalidateUnused(user.getId(), type, clock.instant());
    }

    static String sha256Hex(String raw) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
