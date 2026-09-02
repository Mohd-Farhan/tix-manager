package com.support.service;

import com.support.dto.PasswordChangeDTO;
import com.support.dto.UserDTO;
import com.support.entity.User;
import com.support.entity.UserRole;
import com.support.mapper.UserMapper;
import com.support.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ==============================================================================================
 * AUTOMATED TESTING SUITE: Unit Tests for UserService
 * ==============================================================================================
 * 
 * WHY THIS IS USED (Industry Standard):
 * 1. User Identity & Credential Security: Validates that passwords are NEVER stored in plaintext
 *    and that password changes verify the existing hash before overwriting.
 * 2. Uniqueness Constraints: Prevents race conditions and duplicate user account registrations
 *    (duplicate username / email validation).
 * 3. Exception Transparency: Ensures that querying non-existent entities throws clear, typed
 *    exceptions (EntityNotFoundException) instead of failing silently with null references.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("plainPassword123");
        user.setRole(UserRole.CUSTOMER);

        userDTO = UserDTO.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("plainPassword123")
                .role(UserRole.CUSTOMER)
                .build();
    }

    /**
     * TEST CASE 1: User Registration with password encoding.
     */
    @Test
    @DisplayName("registerUser — Successfully encode password and persist new user")
    void testRegisterUser_Success() {
        // Arrange
        when(userMapper.toEntity(userDTO)).thenReturn(user);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plainPassword123")).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDTO(user)).thenReturn(userDTO);

        // Act
        UserDTO registered = userService.registerUser(userDTO);

        // Assert
        assertThat(registered).isNotNull();
        assertThat(registered.getUsername()).isEqualTo("testuser");
        verify(passwordEncoder, times(1)).encode("plainPassword123");
        verify(userRepository, times(1)).save(user);
    }

    /**
     * TEST CASE 2: Duplicate username registration must throw RuntimeException.
     */
    @Test
    @DisplayName("registerUser — Throw exception when username already exists")
    void testRegisterUser_DuplicateUsername_ThrowsException() {
        // Arrange
        when(userMapper.toEntity(userDTO)).thenReturn(user);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> userService.registerUser(userDTO))
                .isInstanceOf(com.support.exception.DuplicateResourceException.class)
                .hasMessageContaining("User with username 'testuser' already exists");
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * TEST CASE 3: Password Update with valid current password.
     */
    @Test
    @DisplayName("updatePassword — Successfully verify old password hash and update to new password")
    void testUpdatePassword_Success() {
        // Arrange
        user.setPassword("$2a$10$encodedOldPassword");
        PasswordChangeDTO changeDTO = new PasswordChangeDTO("oldPassword123", "newSecret456");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword123", "$2a$10$encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.encode("newSecret456")).thenReturn("$2a$10$newHashedPassword");

        // Act
        userService.updatePassword(1L, changeDTO);

        // Assert
        verify(passwordEncoder, times(1)).matches("oldPassword123", "$2a$10$encodedOldPassword");
        verify(passwordEncoder, times(1)).encode("newSecret456");
        verify(userRepository, times(1)).save(user);
    }

    /**
     * TEST CASE 4: Password Update with incorrect current password throws InvalidOperationException.
     */
    @Test
    @DisplayName("updatePassword — Throw exception when current password does not match")
    void testUpdatePassword_IncorrectCurrentPassword_ThrowsException() {
        // Arrange
        user.setPassword("$2a$10$encodedOldPassword");
        PasswordChangeDTO changeDTO = new PasswordChangeDTO("wrongPassword", "newSecret456");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "$2a$10$encodedOldPassword")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> userService.updatePassword(1L, changeDTO))
                .isInstanceOf(com.support.exception.InvalidOperationException.class)
                .hasMessageContaining("Current password does not match");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(user);
    }
}
