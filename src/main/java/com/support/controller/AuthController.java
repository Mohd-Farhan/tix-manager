package com.support.controller;

import com.support.dto.AuthResponse;
import com.support.dto.LoginDTO;
import com.support.dto.UserDTO;
import com.support.mapper.UserMapper;
import com.support.security.JwtService;
import com.support.security.UserDetailsImpl;
import com.support.service.UserService;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserMapper userMapper;

    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUser(@Valid @RequestBody UserDTO userDto) {
        UserDTO createdUser = userService.registerUser(userDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDTO loginDTO) {
        try {
            // Step 1: Authenticate the user's credentials
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDTO.getUsername(),
                            loginDTO.getPassword()));

            // Step 2: Extract authenticated user details
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            // Step 3: Generate JWT token
            String token = jwtService.generateToken(userDetails);

            // Step 4: Build and return the response
            UserDTO userDto = userMapper.toDTO(userDetails.getUser());
            AuthResponse response = AuthResponse.builder()
                    .token(token)
                    .user(userDto)
                    .build();

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
    }
}

