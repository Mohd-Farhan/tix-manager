package com.support.controller;

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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ==============================================================================================
 * REST CONTROLLER: UserController
 * ==============================================================================================
 * 
 * Manages user profile updates, password changes, agent rosters, and administrative user management.
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Endpoints for profile management, password updates, and user administration")
public class UserController {

    @Autowired
    private UserService userService;

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

    @Operation(summary = "Soft delete user", description = "Flags a user as deleted without dropping historical database records. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User soft deleted"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> softDeleteUser(@PathVariable Long id) {
        userService.softDeleteUser(id);
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
}
