package com.w_s_backend.w_s;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import javax.management.RuntimeErrorException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.w_s_backend.w_s.DTOs.UserRegistrationDTO;
import com.w_s_backend.w_s.Repositories.UserRepository;
import com.w_s_backend.w_s.Services.UserService;
import com.w_s_backend.w_s.models.User;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserRegistrationDTO testDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword123");

        testDTO = new UserRegistrationDTO();
        testDTO.setUsername("testuser");
        testDTO.setEmail("test@example.com");
        testDTO.setPassword("rawPassword123");
    }

    // ==================== createUser ====================

    @Test
    void createUser_ShouldEncodePasswordAndSave() {
        // Arrange
        when(passwordEncoder.encode("rawPassword123")).thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        User result = userService.createUser(testDTO);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("encodedPassword123", result.getPassword());
        verify(passwordEncoder).encode("rawPassword123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_ShouldNotStoreRawPassword() {
        // Arrange
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        User result = userService.createUser(testDTO);

        // Assert
        assertNotEquals("rawPassword123", result.getPassword());
        assertEquals("encodedPassword123", result.getPassword());
    }

    @Test
    void createUser_ShouldCallPasswordEncoderOnce() {
        // Arrange
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        userService.createUser(testDTO);

        // Assert
        verify(passwordEncoder, times(1)).encode(anyString());
    }

    @Test
    void createUser_ShouldSaveUserToRepository() {
        // Arrange
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        userService.createUser(testDTO);

        // Assert
        verify(userRepository, times(1)).save(any(User.class));
    }

    // ==================== findById ====================

    @Test
    void findById_ShouldReturnUser_WhenExists() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.findById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        verify(userRepository).findById(1L);
    }

    @Test
    void findById_ShouldThrowException_WhenNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, 
                () -> userService.findById(999L));
        assertEquals("User not found", ex.getMessage());
        verify(userRepository).findById(999L);
    }

    @Test
    void findById_ShouldThrowRuntimeErrorException_WhenIdIsNull() {
        // Act & Assert
        assertThrows(RuntimeErrorException.class, 
                () -> userService.findById(null));
        // Убеждаемся, что репозиторий НЕ вызывался
        verify(userRepository, never()).findById(any());
    }

    @Test
    void findById_ShouldReturnCorrectUser_WhenMultipleUsersExist() {
        // Arrange
        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setUsername("otheruser");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(anotherUser));

        // Act
        User result1 = userService.findById(1L);
        User result2 = userService.findById(2L);

        // Assert
        assertEquals("testuser", result1.getUsername());
        assertEquals("otheruser", result2.getUsername());
    }

    // ==================== findByUsername ====================

    @Test
    void findByUsername_ShouldReturnUser_WhenExists() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.findByUsername("testuser");

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void findByUsername_ShouldThrowException_WhenNotFound() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, 
                () -> userService.findByUsername("nonexistent"));
        assertEquals("User not found", ex.getMessage());
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void findByUsername_ShouldBeCaseSensitive() {
        // Arrange
        when(userRepository.findByUsername("TestUser")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, 
                () -> userService.findByUsername("TestUser"));
        verify(userRepository).findByUsername("TestUser");
    }


    // ==================== Интеграционные проверки ====================

    @Test
    void createAndFindUser_ShouldReturnSameData() {
        // Arrange
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User saved = i.getArgument(0);
            saved.setId(1L); // Симулируем присвоение ID базой
            return saved;
        });
        when(userRepository.findById(1L)).thenAnswer(i -> {
            // Возвращаем "сохранённого" пользователя
            User user = new User();
            user.setId(1L);
            user.setUsername(testDTO.getUsername());
            user.setEmail(testDTO.getEmail());
            user.setPassword("encodedPassword123");
            return Optional.of(user);
        });

        // Act
        User created = userService.createUser(testDTO);
        User found = userService.findById(created.getId());

        // Assert
        assertEquals(created.getUsername(), found.getUsername());
        assertEquals(created.getEmail(), found.getEmail());
        assertEquals(created.getPassword(), found.getPassword());
    }
}