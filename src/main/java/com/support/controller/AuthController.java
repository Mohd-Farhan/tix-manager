package com.support.controller;

import com.support.dto.AuthResponse;
import com.support.dto.LoginDTO;
import com.support.dto.UserDTO;
import com.support.mapper.UserMapper;
import com.support.security.JwtService;
import com.support.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * ==============================================================================================
 * REST CONTROLLER: AuthController
 * ==============================================================================================
 * 
 * Manages JWT authentication token issuance.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for JWT login")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserMapper userMapper;


    @Operation(summary = "Authenticate user credentials", description = "Validates username and password, then returns a signed stateless JWT token with user details.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated successfully, returns JWT"),
            @ApiResponse(responseCode = "400", description = "Missing or malformed credentials"),
            @ApiResponse(responseCode = "401", description = "Invalid username or password")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginDTO loginDTO) {
        // Step 1: Authenticate the user's credentials against DaoAuthenticationProvider
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDTO.getUsername(),
                        loginDTO.getPassword()));

        // Step 2: Extract authenticated user details
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Step 3: Generate stateless HMAC-SHA256 JWT token
        String token = jwtService.generateToken(userDetails);

        // Step 4: Build and return the response envelope
        UserDTO userDto = userMapper.toDTO(userDetails.getUser());
        AuthResponse response = AuthResponse.builder()
                .token(token)
                .user(userDto)
                .build();

        return ResponseEntity.ok(response);
    }
}
