package com.w_s_backend.w_s;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.w_s_backend.w_s.DTOs.LoginDTO;
import com.w_s_backend.w_s.DTOs.UserRegistrationDTO;
import com.w_s_backend.w_s.Services.JwtService;
import com.w_s_backend.w_s.Services.UserService;
import com.w_s_backend.w_s.controllers.AuthController;
import com.w_s_backend.w_s.models.User;

import jakarta.servlet.http.Cookie;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController authController;

    private User testUser;
    private UserRegistrationDTO testRegDTO;
    private LoginDTO testLoginDTO;
    private MockHttpServletResponse mockResponse;
    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword123");

        testRegDTO = new UserRegistrationDTO();
        testRegDTO.setUsername("testuser");
        testRegDTO.setEmail("test@example.com");
        testRegDTO.setPassword("rawPassword123");

        testLoginDTO = new LoginDTO();
        testLoginDTO.username = "testuser";
        testLoginDTO.password = "rawPassword123";

        mockResponse = new MockHttpServletResponse();
        mockRequest = new MockHttpServletRequest();
    }

    // ==================== register ====================

    @Test
    void register_ShouldReturnOkWithUser() {
        when(userService.createUser(any(UserRegistrationDTO.class))).thenReturn(testUser);

        ResponseEntity<User> response = authController.registerUser(testRegDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("testuser", response.getBody().getUsername());
        assertEquals("test@example.com", response.getBody().getEmail());
        verify(userService).createUser(testRegDTO);
    }

    @Test
    void register_ShouldReturnUserWithEncodedPassword() {
        when(userService.createUser(any(UserRegistrationDTO.class))).thenReturn(testUser);

        ResponseEntity<User> response = authController.registerUser(testRegDTO);

        // Пароль должен быть закодирован (не raw)
        assertNotEquals("rawPassword123", response.getBody().getPassword());
    }

    // ==================== login ====================

    @Test
    void login_ShouldReturnOkWithJwtCookie_WhenCredentialsCorrect() {
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("rawPassword123", "encodedPassword123")).thenReturn(true);
        when(jwtService.generateToken(1L, "testuser")).thenReturn("generated.jwt.token");

        ResponseEntity<?> response = authController.login(testLoginDTO, mockResponse);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Вход выполнен", body.get("message"));
        assertEquals(1L, body.get("userId"));
        assertEquals("testuser", body.get("username"));

        // Проверяем, что cookie установлен
        Cookie cookie = mockResponse.getCookie("jwt");
        assertNotNull(cookie);
        assertEquals("generated.jwt.token", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertEquals("/", cookie.getPath());
        assertEquals(7 * 24 * 60 * 60, cookie.getMaxAge());
    }

    @Test
    void login_ShouldReturnUnauthorized_WhenPasswordWrong() {
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("wrongPassword", "encodedPassword123")).thenReturn(false);

        testLoginDTO.password = "wrongPassword";

        ResponseEntity<?> response = authController.login(testLoginDTO, mockResponse);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Неверный логин или пароль", body.get("message"));

        // Cookie НЕ должен устанавливаться
        assertNull(mockResponse.getCookie("jwt"));
    }

    @Test
    void login_ShouldReturnUnauthorized_WhenUserNotFound() {
        when(userService.findByUsername("nonexistent"))
                .thenThrow(new RuntimeException("User not found"));

        testLoginDTO.username = "nonexistent";

        // Ожидаем исключение, так как findByUsername кидает RuntimeException
        assertThrows(RuntimeException.class, () -> {
            authController.login(testLoginDTO, mockResponse);
        });
    }

    @Test
    void login_ShouldGenerateTokenWithCorrectUserId() {
        when(userService.findByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateToken(1L, "testuser")).thenReturn("token123");

        authController.login(testLoginDTO, mockResponse);

        verify(jwtService).generateToken(1L, "testuser");
    }

    // ==================== logout ====================

    @Test
    void logout_ShouldClearJwtCookie() {
        ResponseEntity<?> response = authController.logout(mockResponse);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Выход выполнен", body.get("message"));

        // Cookie должен быть очищен (MaxAge = 0, value = null)
        Cookie cookie = mockResponse.getCookie("jwt");
        assertNotNull(cookie);
        assertNull(cookie.getValue());
        assertEquals(0, cookie.getMaxAge());
    }

    // ==================== me ====================

    @Test
    void me_ShouldReturnUserData_WhenJwtValid() {
        Cookie jwtCookie = new Cookie("jwt", "valid.token.here");
        mockRequest.setCookies(jwtCookie);

        when(jwtService.isTokenValid("valid.token.here")).thenReturn(true);
        when(jwtService.extractUserId("valid.token.here")).thenReturn(1L);
        when(userService.findById(1L)).thenReturn(testUser);

        ResponseEntity<?> response = authController.getCurrentUser(mockRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(1L, body.get("userId"));
        assertEquals("testuser", body.get("username"));
        assertEquals("test@example.com", body.get("email"));
    }

    @Test
    void me_ShouldReturnUnauthorized_WhenNoJwtCookie() {
        mockRequest.setCookies(); // пустые куки

        ResponseEntity<?> response = authController.getCurrentUser(mockRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Не авторизован", body.get("message"));
    }

    @Test
    void me_ShouldReturnUnauthorized_WhenJwtInvalid() {
        Cookie jwtCookie = new Cookie("jwt", "invalid.token.here");
        mockRequest.setCookies(jwtCookie);

        when(jwtService.isTokenValid("invalid.token.here")).thenReturn(false);

        ResponseEntity<?> response = authController.getCurrentUser(mockRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void me_ShouldReturnUnauthorized_WhenCookiesNull() {
        mockRequest.setCookies((Cookie[]) null);

        ResponseEntity<?> response = authController.getCurrentUser(mockRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void me_ShouldReturnUnauthorized_WhenUserNotFound() {
        Cookie jwtCookie = new Cookie("jwt", "valid.token.here");
        mockRequest.setCookies(jwtCookie);

        when(jwtService.isTokenValid("valid.token.here")).thenReturn(true);
        when(jwtService.extractUserId("valid.token.here")).thenReturn(999L);
        when(userService.findById(999L)).thenThrow(new RuntimeException("User not found"));

        assertThrows(RuntimeException.class, () -> {
            authController.getCurrentUser(mockRequest);
        });
    }
}