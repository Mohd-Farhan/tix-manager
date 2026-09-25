package com.support.controller;

import com.support.dto.AuthResponse;
import com.support.dto.LoginDTO;
import com.support.dto.RefreshTokenRequest;
import com.support.dto.UserDTO;
import com.support.entity.RefreshToken;
import com.support.mapper.UserMapper;
import com.support.security.JwtService;
import com.support.security.LoginRateLimiterService;
import com.support.security.UserDetailsImpl;
import com.support.service.AuditService;
import com.support.service.RefreshTokenService;
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
 * Manages JWT authentication token issuance, refresh token rotation, and session termination.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for JWT login, refresh, and logout")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AuditService auditService;

    @Autowired
    private LoginRateLimiterService loginRateLimiterService;

    @Operation(summary = "Authenticate user credentials", description = "Validates username and password, then returns a signed short-lived JWT token and revocable refresh token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated successfully, returns access and refresh tokens"),
            @ApiResponse(responseCode = "400", description = "Missing or malformed credentials"),
            @ApiResponse(responseCode = "401", description = "Invalid username or password")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        log.info("Login attempt for user: {}", loginDTO.getUsername());

        String clientIp = auditService.extractClientIp(request);

        // Step 0: Pre-authentication brute-force rate limit evaluation (OWASP ASVS v4.0 §2.2.1)
        loginRateLimiterService.checkBlocked(clientIp, loginDTO.getUsername());

        Authentication authentication;
        try {
            // Step 1: Authenticate the user's credentials against DaoAuthenticationProvider
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDTO.getUsername(),
                            loginDTO.getPassword()));
        } catch (BadCredentialsException ex) {
            loginRateLimiterService.recordFailure(clientIp, loginDTO.getUsername());
            auditService.recordLoginFailure(loginDTO.getUsername(), request, "BAD_CREDENTIALS");
            throw ex;
        } catch (Exception ex) {
            loginRateLimiterService.recordFailure(clientIp, loginDTO.getUsername());
            auditService.recordLoginFailure(loginDTO.getUsername(), request, ex.getMessage());
            throw ex;
        }

        // Authentication succeeded: clear rate-limiting failure tracking
        loginRateLimiterService.recordSuccess(clientIp, loginDTO.getUsername());

        // Step 2: Extract authenticated user details
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Step 3: Record login history
        auditService.recordLoginSuccess(userDetails.getUsername(), request);

        // Step 4: Generate short-lived HMAC-SHA256 JWT access token (15m) and database refresh token (7d)
        String token = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUser());
        log.info("User {} successfully authenticated with role {}", userDetails.getUsername(), userDetails.getUser().getRole());

        // Step 5: Build and return the response envelope
        UserDTO userDto = userMapper.toDTO(userDetails.getUser());
        AuthResponse response = AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(900L)
                .user(userDto)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Refresh access token", description = "Rotates refresh token and issues a new short-lived access token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid, expired, or revoked refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(request.getRefreshToken());
        UserDetailsImpl userDetails = new UserDetailsImpl(newRefreshToken.getUser());
        String newAccessToken = jwtService.generateToken(userDetails);

        AuthResponse response = AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(900L)
                .user(userMapper.toDTO(newRefreshToken.getUser()))
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Logout user session", description = "Revokes refresh token and terminates active user session.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            Authentication authentication) {
        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            refreshTokenService.revokeToken(request.getRefreshToken());
        }
        if (authentication != null && authentication.isAuthenticated()) {
            if (authentication.getPrincipal() instanceof UserDetailsImpl udi) {
                refreshTokenService.revokeAllUserTokens(udi.getUser());
            }
            auditService.recordLogout(authentication.getName());
            log.info("User {} logged out successfully", authentication.getName());
        }
        return ResponseEntity.ok().build();
    }
}
