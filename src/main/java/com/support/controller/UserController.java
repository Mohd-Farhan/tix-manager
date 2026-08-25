package com.support.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.support.dto.UserDTO;
import com.support.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/agents")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPPORT_AGENT')")
    public ResponseEntity<List<UserDTO>> getAgents() {
        List<UserDTO> agents = userService.getUsersByRole(com.support.entity.UserRole.SUPPORT_AGENT);
        return ResponseEntity.ok(agents);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long userId) {
        UserDTO user = userService.findById(userId);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> softDeleteUser(@PathVariable Long id) {
        userService.softDeleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getAllUsersIncludingDeleted() {
        List<UserDTO> users = userService.getAllUsersIncludingDeleted();
        return ResponseEntity.ok(users);
    }
}
