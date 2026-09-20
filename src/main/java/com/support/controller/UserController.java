package com.support.controller;

import com.support.dto.BulkUploadHistoryDTO;
import com.support.dto.BulkUploadResultDTO;
import com.support.dto.CreateUserRequest;
import com.support.dto.PasswordChangeDTO;
import com.support.dto.UserDTO;
import com.support.entity.UserRole;
import com.support.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import java.util.List;

/**
 * ==============================================================================================
 * REST CONTROLLER: UserController
 * ==============================================================================================
 * 
 * Manages user profile updates, password changes, agent rosters, and administrative user management.
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Endpoints for profile management, password updates, and user administration")
public class UserController {

    @Autowired
    private UserService userService;

    @Operation(summary = "Create a new user account", description = "Administrative creation of user accounts. Admins can create Agent/Customer; System Admin can create any role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed on payload or hierarchy violation"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Requires ADMIN or SYSTEM_ADMIN role"),
            @ApiResponse(responseCode = "409", description = "Username or email already exists")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> createUser(
            @Valid @RequestBody CreateUserRequest request,
            Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "anonymous";
        log.info("REST: Admin '{}' creating user '{}' with role {}", actor, request.getUsername(), request.getRole());
        UserDTO user = userService.createUser(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @Operation(summary = "Bulk upload users via CSV", description = "Uploads a CSV file with columns: username,email,password,role. Enforces role hierarchy per row and returns a summary report.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bulk upload processed, returns summary report"),
            @ApiResponse(responseCode = "400", description = "File is empty or unreadable"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Requires ADMIN or SYSTEM_ADMIN role")
    })
    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BulkUploadResultDTO> bulkUploadUsers(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "anonymous";
        log.info("REST: Admin '{}' initiated bulk user upload ({})", actor, file.getOriginalFilename());
        BulkUploadResultDTO result = userService.bulkUploadUsersCsv(file, actor);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get bulk upload history", description = "Retrieves immutable audit history records for all CSV bulk uploads. Requires ADMIN or SYSTEM_ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of bulk upload history records retrieved"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Requires ADMIN or SYSTEM_ADMIN role")
    })
    @GetMapping("/bulk-upload/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BulkUploadHistoryDTO>> getBulkUploadHistory() {
        log.info("REST: Querying bulk upload history");
        List<BulkUploadHistoryDTO> history = userService.getBulkUploadHistory();
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "Get paginated bulk upload history", description = "Retrieves paginated immutable audit history records for bulk uploads. Requires ADMIN or SYSTEM_ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of bulk upload history records retrieved"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Requires ADMIN or SYSTEM_ADMIN role")
    })
    @GetMapping("/bulk-upload/history/paged")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<BulkUploadHistoryDTO>> getBulkUploadHistoryPaged(
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        log.info("REST: Querying paginated bulk upload history");
        Page<BulkUploadHistoryDTO> history = userService.getBulkUploadHistory(pageable);
        return ResponseEntity.ok(history);
    }


    @Operation(summary = "Get all support agents", description = "Fetches the list of active users with the SUPPORT_AGENT role for ticket assignment.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of agents retrieved"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Requires SUPPORT_AGENT or ADMIN role")
    })
    @GetMapping("/agents")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPPORT_AGENT')")
    public ResponseEntity<List<UserDTO>> getAgents() {
        List<UserDTO> agents = userService.getUsersByRole(UserRole.SUPPORT_AGENT);
        return ResponseEntity.ok(agents);
    }

    @Operation(summary = "Get user profile by ID", description = "Retrieves user account details. Users can only fetch their own profile unless they are an ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile retrieved"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Cannot access other users' profiles"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long userId) {
        UserDTO user = userService.findById(userId);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Change user password", description = "Verifies the current password and encrypts the new password with BCrypt.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password successfully changed"),
            @ApiResponse(responseCode = "400", description = "Current password does not match or new password fails validation"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Cannot change another user's password"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/{userId}/password")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<Void> updatePassword(
            @PathVariable Long userId,
            @Valid @RequestBody PasswordChangeDTO dto) {
        log.info("REST: Password update requested for userId={}", userId);
        userService.updatePassword(userId, dto);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update user profile", description = "Updates username and email details for the specified user account.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile successfully updated"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Access denied"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long userId,
            @RequestBody UserDTO userDTO) {
        UserDTO updated = userService.updateUser(userId, userDTO);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Soft delete user", description = "Flags a user as deleted without dropping historical database records. Enforces role hierarchy.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User soft deleted"),
            @ApiResponse(responseCode = "400", description = "Cannot deactivate own account or hierarchy violation"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Requires ADMIN or SYSTEM_ADMIN role"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> softDeleteUser(
            @PathVariable Long id,
            Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "anonymous";
        log.warn("REST: Admin '{}' requested soft-delete for userId={}", actor, id);
        userService.softDeleteUser(id, actor);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all users including deleted", description = "Administrative query to audit all users. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of all users retrieved"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Requires ADMIN role")
    })
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getAllUsersIncludingDeleted() {
        List<UserDTO> users = userService.getAllUsersIncludingDeleted();
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Get paginated active users", description = "Returns pageable active users with configurable page size, number, and sort order.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of active users retrieved"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Requires ADMIN role")
    })
    @GetMapping("/paged")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserDTO>> getAllUsersPaged(
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<UserDTO> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Get paginated users including deleted", description = "Administrative query to audit all users with pagination. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of all users retrieved"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Requires ADMIN role")
    })
    @GetMapping("/admin/paged")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserDTO>> getAllUsersIncludingDeletedPaged(
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<UserDTO> users = userService.getAllUsersIncludingDeleted(pageable);
        return ResponseEntity.ok(users);
    }
}
