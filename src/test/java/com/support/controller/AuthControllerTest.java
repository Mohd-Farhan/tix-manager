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
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private com.support.service.AuditService auditService;

    @MockBean
    private com.support.security.LoginRateLimiterService loginRateLimiterService;

    @MockBean
    private com.support.service.RefreshTokenService refreshTokenService;


    /**
     * TEST CASE 3: Successful Login returns 200 OK and JWT Token.
     */
    @Test
    @DisplayName("POST /api/auth/login — Return 200 OK, JWT Token, and Refresh Token upon valid credentials")
    void testLogin_Success() throws Exception {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("admin");
        loginDTO.setPassword("admin123");

        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setRole(UserRole.ADMIN);
        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        com.support.entity.RefreshToken mockRt = com.support.entity.RefreshToken.builder()
                .id(10L)
                .token("mock-refresh-token-uuid")
                .user(user)
                .expiryDate(java.time.Instant.now().plusSeconds(604800))
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateToken(userDetails)).thenReturn("mock.jwt.token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(mockRt);

        UserDTO userDto = UserDTO.builder().id(1L).username("admin").role(UserRole.ADMIN).build();
        when(userMapper.toDTO(user)).thenReturn(userDto);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock.jwt.token"))
                .andExpect(jsonPath("$.refreshToken").value("mock-refresh-token-uuid"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
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

    /**
     * TEST CASE 5: Login when rate limited returns 429 Too Many Requests.
     */
    @Test
    @DisplayName("POST /api/auth/login — Return 429 TOO_MANY_REQUESTS when account is locked")
    void testLogin_AccountLocked() throws Exception {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("admin");
        loginDTO.setPassword("password");

        org.mockito.Mockito.doThrow(new com.support.exception.AccountLockedException("Account temporarily locked", 15))
                .when(loginRateLimiterService).checkBlocked(any(), any());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value("Account temporarily locked"));
    }

    /**
     * TEST CASE 6: Token refresh succeeds with valid refresh token.
     */
    @Test
    @DisplayName("POST /api/auth/refresh — Return 200 OK and rotated tokens")
    void testRefresh_Success() throws Exception {
        com.support.dto.RefreshTokenRequest request = new com.support.dto.RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");

        User user = new User();
        user.setId(2L);
        user.setUsername("agent");
        user.setRole(UserRole.SUPPORT_AGENT);

        com.support.entity.RefreshToken newRt = com.support.entity.RefreshToken.builder()
                .id(11L)
                .token("new-rotated-refresh-token")
                .user(user)
                .expiryDate(java.time.Instant.now().plusSeconds(604800))
                .build();

        when(refreshTokenService.rotateRefreshToken("valid-refresh-token")).thenReturn(newRt);
        when(jwtService.generateToken(any(UserDetailsImpl.class))).thenReturn("new.access.token");

        UserDTO userDto = UserDTO.builder().id(2L).username("agent").role(UserRole.SUPPORT_AGENT).build();
        when(userMapper.toDTO(user)).thenReturn(userDto);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new.access.token"))
                .andExpect(jsonPath("$.refreshToken").value("new-rotated-refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.username").value("agent"));
    }

    /**
     * TEST CASE 7: Token refresh fails with revoked or expired token (returns 401 Unauthorized).
     */
    @Test
    @DisplayName("POST /api/auth/refresh — Return 401 UNAUTHORIZED when refresh token revoked or invalid")
    void testRefresh_InvalidToken() throws Exception {
        com.support.dto.RefreshTokenRequest request = new com.support.dto.RefreshTokenRequest();
        request.setRefreshToken("revoked-token");

        when(refreshTokenService.rotateRefreshToken("revoked-token"))
                .thenThrow(new com.support.exception.RefreshTokenException("Refresh token was previously revoked."));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Refresh token was previously revoked."));
    }

    /**
     * TEST CASE 8: Logout returns 200 OK and revokes token.
     */
    @Test
    @DisplayName("POST /api/auth/logout — Return 200 OK")
    void testLogout_Success() throws Exception {
        com.support.dto.RefreshTokenRequest request = new com.support.dto.RefreshTokenRequest();
        request.setRefreshToken("user-refresh-token");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(refreshTokenService).revokeToken("user-refresh-token");
    }
}
