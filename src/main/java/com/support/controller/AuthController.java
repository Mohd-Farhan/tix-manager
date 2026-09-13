package com.support.controller;

import com.support.dto.AuthResponse;
import com.support.dto.LoginDTO;
import com.support.dto.UserDTO;
import com.support.mapper.UserMapper;
import com.support.security.JwtService;
import com.support.security.UserDetailsImpl;
import com.support.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * ==============================================================================================
 * REST CONTROLLER: AuthController
 * ==============================================================================================
 * 
 * Manages JWT authentication token issuance and session audit tracking.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for JWT login and logout")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AuditService auditService;

    @Operation(summary = "Authenticate user credentials", description = "Validates username and password, then returns a signed stateless JWT token with user details.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated successfully, returns JWT"),
            @ApiResponse(responseCode = "400", description = "Missing or malformed credentials"),
            @ApiResponse(responseCode = "401", description = "Invalid username or password")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        log.info("Login attempt for user: {}", loginDTO.getUsername());

        Authentication authentication;
        try {
            // Step 1: Authenticate the user's credentials against DaoAuthenticationProvider
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDTO.getUsername(),
                            loginDTO.getPassword()));
        } catch (BadCredentialsException ex) {
            auditService.recordLoginFailure(loginDTO.getUsername(), request, "BAD_CREDENTIALS");
            throw ex;
        } catch (Exception ex) {
            auditService.recordLoginFailure(loginDTO.getUsername(), request, ex.getMessage());
            throw ex;
        }

        // Step 2: Extract authenticated user details
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Step 3: Record login history
        auditService.recordLoginSuccess(userDetails.getUsername(), request);

        // Step 4: Generate stateless HMAC-SHA256 JWT token
        String token = jwtService.generateToken(userDetails);
        log.info("User {} successfully authenticated with role {}", userDetails.getUsername(), userDetails.getUser().getRole());

        // Step 5: Build and return the response envelope
        UserDTO userDto = userMapper.toDTO(userDetails.getUser());
        AuthResponse response = AuthResponse.builder()
                .token(token)
                .user(userDto)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Logout user session", description = "Records logout timestamp in login history for the currently authenticated user.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            auditService.recordLogout(authentication.getName());
            log.info("User {} logged out successfully", authentication.getName());
        }
        return ResponseEntity.ok().build();
    }
}
