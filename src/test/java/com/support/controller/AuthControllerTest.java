package com.support.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.support.config.GlobalExceptionHandler;
import com.support.dto.LoginDTO;
import com.support.dto.UserDTO;
import com.support.entity.User;
import com.support.entity.UserRole;
import com.support.mapper.UserMapper;
import com.support.security.JwtService;
import com.support.security.UserDetailsImpl;
import com.support.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ==============================================================================================
 * AUTOMATED TESTING SUITE: Controller Slice Test for AuthController
 * ==============================================================================================
 * 
 * WHY THIS IS USED (Industry Standard):
 * 1. Web Layer Verification: Tests HTTP contracts (request serialization, status codes,
 *    response bodies, validation error triggers) without spinning up the full application.
 * 2. Auth Flow Validation: Validates registration endpoint returns 201 Created and login returns
 *    valid JWT bearer tokens in AuthResponse DTO.
 * 3. Security Error Handling: Verifies 401 Unauthorized is returned upon BadCredentialsException.
 */
@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filter chain for isolated controller test
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserMapper userMapper;

    /**
     * TEST CASE 1: Register endpoint with valid payload returns 201 Created.
     */
    @Test
    @DisplayName("POST /api/auth/register — Successfully register user and return 201 CREATED")
    void testRegisterUser_Success() throws Exception {
        UserDTO inputDto = UserDTO.builder()
                .username("new_customer")
                .email("customer@example.com")
                .password("Password123")
                .role(UserRole.CUSTOMER)
                .build();

        UserDTO createdDto = UserDTO.builder()
                .id(10L)
                .username("new_customer")
                .email("customer@example.com")
                .role(UserRole.CUSTOMER)
                .build();

        when(userService.registerUser(any(UserDTO.class))).thenReturn(createdDto);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.username").value("new_customer"))
                .andExpect(jsonPath("$.email").value("customer@example.com"));
    }

    /**
     * TEST CASE 2: Register endpoint with invalid input returns 400 Bad Request.
     */
    @Test
    @DisplayName("POST /api/auth/register — Return 400 BAD REQUEST when username/email validation fails")
    void testRegisterUser_ValidationFailure() throws Exception {
        UserDTO invalidDto = UserDTO.builder()
                .username("ab") // min size is 3
                .email("invalid-email-address")
                .password("")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    /**
     * TEST CASE 3: Successful Login returns 200 OK and JWT Token.
     */
    @Test
    @DisplayName("POST /api/auth/login — Return 200 OK and JWT Token upon valid credentials")
    void testLogin_Success() throws Exception {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("admin");
        loginDTO.setPassword("admin123");

        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setRole(UserRole.ADMIN);
        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateToken(userDetails)).thenReturn("mock.jwt.token");

        UserDTO userDto = UserDTO.builder().id(1L).username("admin").role(UserRole.ADMIN).build();
        when(userMapper.toDTO(user)).thenReturn(userDto);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock.jwt.token"))
                .andExpect(jsonPath("$.user.username").value("admin"));
    }

    /**
     * TEST CASE 4: Login with bad credentials returns 401 Unauthorized.
     */
    @Test
    @DisplayName("POST /api/auth/login — Return 401 UNAUTHORIZED for invalid password")
    void testLogin_BadCredentials() throws Exception {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("admin");
        loginDTO.setPassword("wrong_password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
