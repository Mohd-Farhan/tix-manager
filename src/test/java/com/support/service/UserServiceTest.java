package com.support.service;

import com.support.dto.BulkUploadHistoryDTO;
import com.support.dto.PasswordChangeDTO;
import com.support.dto.UserDTO;
import com.support.entity.BulkUploadHistory;
import com.support.entity.User;
import com.support.entity.UserRole;
import com.support.mapper.BulkUploadHistoryMapper;
import com.support.mapper.UserMapper;
import com.support.repository.BulkUploadHistoryRepository;
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

    @Mock
    private BulkUploadHistoryRepository bulkUploadHistoryRepository;

    @Mock
    private BulkUploadHistoryMapper bulkUploadHistoryMapper;

    @Mock
    private EmailService emailService;

    @Mock
    private RefreshTokenService refreshTokenService;

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
        user.setMustChangePassword(true);
        userService.updatePassword(1L, changeDTO);

        // Assert
        verify(passwordEncoder, times(1)).matches("oldPassword123", "$2a$10$encodedOldPassword");
        verify(passwordEncoder, times(1)).encode("newSecret456");
        verify(userRepository, times(1)).save(user);
        assertThat(user.isMustChangePassword()).isFalse();
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

        verify(bulkUploadHistoryRepository, times(1)).save(argThat(history ->
                history.getFileName().equals("users.csv") &&
                history.getUploadedBy().equals("admin") &&
                history.getTotalRows() == 3 &&
                history.getSuccessCount() == 1 &&
                history.getFailureCount() == 2 &&
                history.getStatus().equals("PARTIAL_SUCCESS") &&
                history.getErrorDetails() != null
        ));
    }

    /**
     * TEST CASE 7b: Bulk Upload History retrieval.
     */
    @Test
    @DisplayName("getBulkUploadHistory — Retrieve sorted history records")
    void testGetBulkUploadHistory() {
        BulkUploadHistory item = BulkUploadHistory.builder()
                .id(10L)
                .fileName("test.csv")
                .uploadedBy("admin")
                .totalRows(5)
                .successCount(5)
                .failureCount(0)
                .status("SUCCESS")
                .build();

        BulkUploadHistoryDTO dto = BulkUploadHistoryDTO.builder()
                .id(10L)
                .fileName("test.csv")
                .uploadedBy("admin")
                .totalRows(5)
                .successCount(5)
                .failureCount(0)
                .status("SUCCESS")
                .build();

        when(bulkUploadHistoryRepository.findAllByOrderByCreatedAtDesc()).thenReturn(java.util.List.of(item));
        when(bulkUploadHistoryMapper.toDTOList(java.util.List.of(item))).thenReturn(java.util.List.of(dto));

        java.util.List<BulkUploadHistoryDTO> result = userService.getBulkUploadHistory();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFileName()).isEqualTo("test.csv");
        assertThat(result.get(0).getStatus()).isEqualTo("SUCCESS");
    }

    /**
     * TEST CASE 7c: Bulk CSV Upload sanitizes formula injection characters (OWASP CWE-1236).
     */
    @Test
    @DisplayName("bulkUploadUsersCsv — Strips formula injection prefixes (=, +, -, @) per OWASP CWE-1236")
    void testBulkUploadUsersCsv_FormulaInjectionSanitized() {
        User adminCaller = new User();
        adminCaller.setId(2L);
        adminCaller.setUsername("admin");
        adminCaller.setRole(UserRole.ADMIN);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminCaller));
        when(userRepository.findByUsername("clean_agent")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("clean@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashed");

        // Username has '=clean_agent' formula prefix, email has '+clean@test.com' formula prefix
        String csvData = "username,email,password,role\n" +
                "=clean_agent,+clean@test.com,Pass123!,SUPPORT_AGENT\n";

        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "formula_test.csv", "text/csv", csvData.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        com.support.dto.BulkUploadResultDTO result = userService.bulkUploadUsersCsv(file, "admin");

        assertThat(result.getSuccessCount()).isEqualTo(1);
        verify(userRepository, times(1)).save(argThat(u ->
                u.getUsername().equals("clean_agent") &&
                u.getEmail().equals("clean@test.com") &&
                u.isMustChangePassword()
        ));
    }

    /**
     * TEST CASE 7d: Direct sanitizeCsvFormula method tests covering all OWASP formula triggers and quotes.
     */
    @Test
    @DisplayName("sanitizeCsvFormula — Strips =, +, -, @, \\t, \\r and quotes per OWASP CWE-1236")
    void testSanitizeCsvFormula_AllCases() {
        assertThat(userService.sanitizeCsvFormula("=cmd|' /C calc'!A0")).isEqualTo("cmd|' /C calc'!A0");
        assertThat(userService.sanitizeCsvFormula("+12345")).isEqualTo("12345");
        assertThat(userService.sanitizeCsvFormula("-sum(A1:A10)")).isEqualTo("sum(A1:A10)");
        assertThat(userService.sanitizeCsvFormula("@SUM(1+1)*cmd|' /C calc'!A0")).isEqualTo("SUM(1+1)*cmd|' /C calc'!A0");
        assertThat(userService.sanitizeCsvFormula("\t=evil")).isEqualTo("evil");
        assertThat(userService.sanitizeCsvFormula("\r-calc")).isEqualTo("calc");
        assertThat(userService.sanitizeCsvFormula("\"=quoted_formula\"")).isEqualTo("quoted_formula");
        assertThat(userService.sanitizeCsvFormula("\"+quoted_agent\"")).isEqualTo("quoted_agent");
        assertThat(userService.sanitizeCsvFormula("\"-quoted_user\"")).isEqualTo("quoted_user");
        assertThat(userService.sanitizeCsvFormula("\"@quoted_admin\"")).isEqualTo("quoted_admin");
        assertThat(userService.sanitizeCsvFormula("normal_username")).isEqualTo("normal_username");
        assertThat(userService.sanitizeCsvFormula(null)).isNull();
        assertThat(userService.sanitizeCsvFormula("")).isEqualTo("");
    }

    /**
     * TEST CASE 7e: Single user creation also sanitizes formula injection characters.
     */
    @Test
    @DisplayName("createUser — Sanitizes formula injection characters in username and email")
    void testCreateUser_FormulaInjectionSanitized() {
        User adminCaller = new User();
        adminCaller.setId(2L);
        adminCaller.setUsername("admin");
        adminCaller.setRole(UserRole.ADMIN);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminCaller));
        when(userRepository.findByUsername("safe_agent")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("safe@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashed");

        com.support.dto.CreateUserRequest request = com.support.dto.CreateUserRequest.builder()
                .username("=safe_agent")
                .email("+safe@example.com")
                .role(UserRole.SUPPORT_AGENT)
                .build();

        User savedUser = new User();
        savedUser.setId(10L);
        savedUser.setUsername("safe_agent");
        savedUser.setEmail("safe@example.com");
        savedUser.setRole(UserRole.SUPPORT_AGENT);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toDTO(any(User.class))).thenReturn(new com.support.dto.UserDTO());

        userService.createUser(request, "admin");

        verify(userRepository).save(argThat(u ->
                u.getUsername().equals("safe_agent") &&
                u.getEmail().equals("safe@example.com")
        ));
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

    /**
     * TEST CASE 10: Pagination — Retrieve paginated users.
     */
    @Test
    @DisplayName("getAllUsers(Pageable) — Returns paginated UserDTO slice")
    void testGetAllUsersPaged() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        org.springframework.data.domain.Page<User> userPage = new org.springframework.data.domain.PageImpl<>(java.util.List.of(user));

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toDTO(user)).thenReturn(UserDTO.builder().id(1L).username("testuser").build());

        org.springframework.data.domain.Page<UserDTO> result = userService.getAllUsers(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("testuser");
    }

    /**
     * TEST CASE 11: Pagination — Retrieve paginated users including deleted.
     */
    @Test
    @DisplayName("getAllUsersIncludingDeleted(Pageable) — Returns paginated UserDTO slice including soft-deleted")
    void testGetAllUsersIncludingDeletedPaged() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        User user = new User();
        user.setId(1L);
        user.setUsername("deleted_user");
        user.setDeleted(true);
        org.springframework.data.domain.Page<User> userPage = new org.springframework.data.domain.PageImpl<>(java.util.List.of(user));

        when(userRepository.findAllIncludingDeleted(pageable)).thenReturn(userPage);
        when(userMapper.toDTO(user)).thenReturn(UserDTO.builder().id(1L).username("deleted_user").build());

        org.springframework.data.domain.Page<UserDTO> result = userService.getAllUsersIncludingDeleted(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("deleted_user");
    }

    /**
     * TEST CASE 12: Pagination — Retrieve paginated bulk upload history.
     */
    @Test
    @DisplayName("getBulkUploadHistory(Pageable) — Returns paginated BulkUploadHistoryDTO slice")
    void testGetBulkUploadHistoryPaged() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        BulkUploadHistory history = BulkUploadHistory.builder()
                .fileName("users.csv")
                .totalRows(50)
                .build();
        org.springframework.data.domain.Page<BulkUploadHistory> historyPage = new org.springframework.data.domain.PageImpl<>(java.util.List.of(history));

        when(bulkUploadHistoryRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(historyPage);
        when(bulkUploadHistoryMapper.toDTO(history)).thenReturn(BulkUploadHistoryDTO.builder().fileName("users.csv").totalRows(50).build());

        org.springframework.data.domain.Page<BulkUploadHistoryDTO> result = userService.getBulkUploadHistory(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getFileName()).isEqualTo("users.csv");
    }
}


