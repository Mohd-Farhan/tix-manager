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

    @Mock
    private AuditService auditService;

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

    /**
     * TEST CASE 5: Admin cannot create Admin or System Admin account (Privilege Escalation Protection).
     */
    @Test
    @DisplayName("createUser — Admin cannot create Admin or System Admin accounts")
    void testCreateUser_AdminCreatesAdmin_ThrowsHierarchyException() {
        User adminCaller = new User();
        adminCaller.setId(2L);
        adminCaller.setUsername("regular_admin");
        adminCaller.setRole(UserRole.ADMIN);

        when(userRepository.findByUsername("regular_admin")).thenReturn(Optional.of(adminCaller));

        com.support.dto.CreateUserRequest request = com.support.dto.CreateUserRequest.builder()
                .username("new_admin")
                .email("newadmin@test.com")
                .role(UserRole.ADMIN)
                .build();

        assertThatThrownBy(() -> userService.createUser(request, "regular_admin"))
                .isInstanceOf(com.support.exception.InvalidOperationException.class)
                .hasMessageContaining("Admins cannot create Admin or System Admin accounts");
    }

    /**
     * TEST CASE 6: System Admin can create Admin account.
     */
    @Test
    @DisplayName("createUser — System Admin can create Admin account")
    void testCreateUser_SystemAdminCreatesAdmin_Success() {
        User sysAdminCaller = new User();
        sysAdminCaller.setId(1L);
        sysAdminCaller.setUsername("sysadmin");
        sysAdminCaller.setRole(UserRole.SYSTEM_ADMIN);

        when(userRepository.findByUsername("sysadmin")).thenReturn(Optional.of(sysAdminCaller));
        when(userRepository.findByUsername("brand_new_admin")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@brand.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toDTO(any(User.class))).thenReturn(UserDTO.builder().username("brand_new_admin").role(UserRole.ADMIN).build());

        com.support.dto.CreateUserRequest request = com.support.dto.CreateUserRequest.builder()
                .username("brand_new_admin")
                .email("admin@brand.com")
                .role(UserRole.ADMIN)
                .build();

        UserDTO created = userService.createUser(request, "sysadmin");
        assertThat(created).isNotNull();
        assertThat(created.getUsername()).isEqualTo("brand_new_admin");
        verify(userRepository, times(1)).save(any(User.class));
    }

    /**
     * TEST CASE 7: Bulk CSV Upload processes valid rows and reports row failures.
     */
    @Test
    @DisplayName("bulkUploadUsersCsv — Process valid rows and report invalid rows without failure")
    void testBulkUploadUsersCsv_SuccessAndPartialFailure() {
        User adminCaller = new User();
        adminCaller.setId(2L);
        adminCaller.setUsername("admin");
        adminCaller.setRole(UserRole.ADMIN);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminCaller));
        when(userRepository.findByUsername("valid_agent")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("valid@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashed");

        String csvData = "username,email,password,role\n" +
                "valid_agent,valid@test.com,Pass123!,SUPPORT_AGENT\n" +
                "bad_admin,bad@test.com,,ADMIN\n" + // Admin cannot create ADMIN
                "invalid_email,notanemail,,CUSTOMER\n";

        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "users.csv", "text/csv", csvData.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        com.support.dto.BulkUploadResultDTO result = userService.bulkUploadUsersCsv(file, "admin");

        assertThat(result.getTotalRows()).isEqualTo(3);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailureCount()).isEqualTo(2);
        assertThat(result.getErrors()).hasSize(2);
        assertThat(result.getErrors().get(0)).contains("Admins cannot create ADMIN accounts");
        assertThat(result.getErrors().get(1)).contains("Invalid email format");
    }

    /**
     * TEST CASE 8: Soft Delete Hierarchy — Admin cannot deactivate another Admin.
     */
    @Test
    @DisplayName("softDeleteUser — Admin cannot deactivate Admin account")
    void testSoftDeleteUser_AdminCannotDeactivateAdmin() {
        User adminCaller = new User();
        adminCaller.setId(2L);
        adminCaller.setUsername("admin");
        adminCaller.setRole(UserRole.ADMIN);

        User targetAdmin = new User();
        targetAdmin.setId(3L);
        targetAdmin.setUsername("target_admin");
        targetAdmin.setRole(UserRole.ADMIN);

        when(userRepository.findById(3L)).thenReturn(Optional.of(targetAdmin));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminCaller));

        assertThatThrownBy(() -> userService.softDeleteUser(3L, "admin"))
                .isInstanceOf(com.support.exception.InvalidOperationException.class)
                .hasMessageContaining("Admins cannot deactivate Admin or System Admin accounts");
    }

    /**
     * TEST CASE 9: Soft Delete — User cannot deactivate self.
     */
    @Test
    @DisplayName("softDeleteUser — User cannot deactivate self")
    void testSoftDeleteUser_CannotDeactivateSelf() {
        User adminCaller = new User();
        adminCaller.setId(2L);
        adminCaller.setUsername("admin");
        adminCaller.setRole(UserRole.ADMIN);

        when(userRepository.findById(2L)).thenReturn(Optional.of(adminCaller));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminCaller));

        assertThatThrownBy(() -> userService.softDeleteUser(2L, "admin"))
                .isInstanceOf(com.support.exception.InvalidOperationException.class)
                .hasMessageContaining("You cannot deactivate your own account");
    }
}

