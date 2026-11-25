package com.recipehub.user;

import com.recipehub.common.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    static final Instant NOW = Instant.parse("2026-11-25T01:00:00Z");

    @Mock UserTokenRepository repo;
    final User user = new User("u@x.com", "u", "h", "U");
    TokenService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new TokenService(repo, Clock.fixed(NOW, ZoneOffset.UTC));
        org.mockito.Mockito.lenient().when(repo.markUsed(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(1);
    }

    UserToken stored(String raw, TokenType type, Instant expiresAt) {
        UserToken t = new UserToken(user, type, TokenService.sha256Hex(raw), expiresAt);
        when(repo.findByTokenHash(TokenService.sha256Hex(raw))).thenReturn(Optional.of(t));
        return t;
    }

    @Test
    void storesHashNotRawToken() {
        String raw = service.issue(user, TokenType.VERIFY_EMAIL);
        var captor = ArgumentCaptor.forClass(UserToken.class);
        verify(repo).save(captor.capture());
        UserToken t = captor.getValue();
        assertThat(raw).hasSizeGreaterThanOrEqualTo(43).matches("[A-Za-z0-9_-]+");
        assertThat(t.getTokenHash()).isEqualTo(TokenService.sha256Hex(raw)).isNotEqualTo(raw).hasSize(64);
        assertThat(t.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
    }

    @Test
    void resetTokenExpiresIn30Minutes() {
        service.issue(user, TokenType.RESET_PASSWORD);
        var captor = ArgumentCaptor.forClass(UserToken.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
    }

    @Test
    void consumeExpiredFails() {
        stored("raw", TokenType.VERIFY_EMAIL, NOW.minusSeconds(1));
        assertThatThrownBy(() -> service.consume("raw", TokenType.VERIFY_EMAIL))
                .hasFieldOrPropertyWithValue("code", ErrorCode.INVALID_TOKEN);
    }

    @Test
    void consumeUsedFails() {
        stored("raw", TokenType.VERIFY_EMAIL, NOW.plusSeconds(60)).setUsedAt(NOW.minusSeconds(5));
        assertThatThrownBy(() -> service.consume("raw", TokenType.VERIFY_EMAIL))
                .hasFieldOrPropertyWithValue("code", ErrorCode.INVALID_TOKEN);
    }

    @Test
    void consumeWrongTypeFails() {
        stored("raw", TokenType.VERIFY_EMAIL, NOW.plusSeconds(60));
        assertThatThrownBy(() -> service.consume("raw", TokenType.RESET_PASSWORD))
                .hasFieldOrPropertyWithValue("code", ErrorCode.INVALID_TOKEN);
    }

    @Test
    void consumeUnknownFails() {
        assertThatThrownBy(() -> service.consume("nope", TokenType.VERIFY_EMAIL))
                .hasFieldOrPropertyWithValue("code", ErrorCode.INVALID_TOKEN);
    }

    @Test
    void consumeMarksUsed() {
        UserToken t = stored("raw", TokenType.VERIFY_EMAIL, NOW.plusSeconds(60));
        assertThat(service.consume("raw", TokenType.VERIFY_EMAIL)).isSameAs(user);
        assertThat(t.getUsedAt()).isEqualTo(NOW);
    }

    @Test
    void consumeLosingRaceFails() {
        stored("raw", TokenType.VERIFY_EMAIL, NOW.plusSeconds(60));
        when(repo.markUsed(null, NOW)).thenReturn(0); // request song song đã đánh dấu trước
        assertThatThrownBy(() -> service.consume("raw", TokenType.VERIFY_EMAIL))
                .hasFieldOrPropertyWithValue("code", ErrorCode.INVALID_TOKEN);
    }
}
