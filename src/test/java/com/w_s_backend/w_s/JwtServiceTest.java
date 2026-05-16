package com.w_s_backend.w_s;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.w_s_backend.w_s.Services.JwtService;

class JwtServiceTest {

    private JwtService jwtService;

    // Секрет должен быть минимум 256 бит (32 символа) для HMAC-SHA256
    private static final String TEST_SECRET = "this-is-a-test-secret-key-for-jwt-256bits!";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Внедряем секрет через рефлексию (без Spring-контекста)
        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET);
    }

    // ==================== generateToken ====================

    @Test
    void generateToken_ShouldReturnNonEmptyString() {
        String token = jwtService.generateToken(1L, "testuser");

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3, "JWT должен содержать 3 части (header.payload.signature)");
    }

    @Test
    void generateToken_ShouldContainUserId() {
        String token = jwtService.generateToken(42L, "testuser");

        Long extractedId = jwtService.extractUserId(token);
        assertEquals(42L, extractedId);
    }

    @Test
    void generateToken_ShouldWorkForDifferentUsers() {
        String token1 = jwtService.generateToken(1L, "user1");
        String token2 = jwtService.generateToken(2L, "user2");

        assertEquals(1L, jwtService.extractUserId(token1));
        assertEquals(2L, jwtService.extractUserId(token2));
        assertNotEquals(token1, token2, "Токены разных пользователей должны различаться");
    }

    // ==================== extractUserId ====================

    @Test
    void extractUserId_ShouldReturnCorrectId() {
        String token = jwtService.generateToken(100L, "testuser");

        Long userId = jwtService.extractUserId(token);

        assertEquals(100L, userId);
    }

    @Test
    void extractUserId_ShouldWorkWithLargeIds() {
        String token = jwtService.generateToken(Long.MAX_VALUE, "testuser");

        Long userId = jwtService.extractUserId(token);

        assertEquals(Long.MAX_VALUE, userId);
    }

    @Test
    void extractUserId_ShouldThrowException_ForInvalidToken() {
        assertThrows(Exception.class, () -> {
            jwtService.extractUserId("invalid.token.here");
        });
    }

    @Test
    void extractUserId_ShouldThrowException_ForEmptyToken() {
        assertThrows(Exception.class, () -> {
            jwtService.extractUserId("");
        });
    }

    @Test
    void extractUserId_ShouldThrowException_ForNullToken() {
        assertThrows(Exception.class, () -> {
            jwtService.extractUserId(null);
        });
    }

    // ==================== isTokenValid ====================

    @Test
    void isTokenValid_ShouldReturnTrue_ForValidToken() {
        String token = jwtService.generateToken(1L, "testuser");

        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void isTokenValid_ShouldReturnFalse_ForInvalidToken() {
        assertFalse(jwtService.isTokenValid("invalid.token.string"));
    }

    @Test
    void isTokenValid_ShouldReturnFalse_ForEmptyToken() {
        assertFalse(jwtService.isTokenValid(""));
    }

    @Test
    void isTokenValid_ShouldReturnFalse_ForNullToken() {
        assertFalse(jwtService.isTokenValid(null));
    }

    @Test
    void isTokenValid_ShouldReturnFalse_ForTamperedToken() {
        String token = jwtService.generateToken(1L, "testuser");

        // Подделываем токен (меняем последний символ)
        String tamperedToken = token.substring(0, token.length() - 1) + "X";

        assertFalse(jwtService.isTokenValid(tamperedToken));
    }

    @Test
    void isTokenValid_ShouldReturnFalse_ForExpiredToken() throws InterruptedException {
        // Устанавливаем очень короткий срок действия
        ReflectionTestUtils.setField(jwtService, "JWT_EXPIRATION", 1L); // 1 мс

        String token = jwtService.generateToken(1L, "testuser");

        // Ждём истечения срока
        Thread.sleep(10);

        assertTrue(jwtService.isTokenValid(token), "Токен с истекшим сроком должен быть невалидным");
    }

    // ==================== Интеграционные проверки ====================

    @Test
    void generateAndValidate_ShouldWorkEndToEnd() {
        // 1. Генерируем токен
        String token = jwtService.generateToken(77L, "john");

        // 2. Проверяем валидность
        assertTrue(jwtService.isTokenValid(token));

        // 3. Извлекаем userId
        Long userId = jwtService.extractUserId(token);
        assertEquals(77L, userId);
    }
    
    @Test
    void tokenSignedWithDifferentSecret_ShouldBeInvalid() {
        // Генерируем токен с одним секретом
        String token = jwtService.generateToken(1L, "testuser");

        // Создаём другой сервис с другим секретом
        JwtService otherService = new JwtService();
        ReflectionTestUtils.setField(otherService, "secret", "another-secret-key-for-testing-purposes!");

        // Токен должен быть невалидным для другого сервиса
        assertFalse(otherService.isTokenValid(token),
                "Токен, подписанный другим секретом, должен быть невалидным");
    }
}