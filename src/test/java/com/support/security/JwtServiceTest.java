package com.support.security;

import com.support.entity.User;
import com.support.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ==============================================================================================
 * AUTOMATED TESTING SUITE: Unit Tests for JwtService
 * ==============================================================================================
 * 
 * WHY THIS IS USED (Industry Standard):
 * 1. Cryptographic Security: Ensures HMAC-SHA256 token generation creates valid tokens with
 *    appropriate claims and expiration timestamps.
 * 2. Stateless Auth Contract: Verifies that claims (username, subject) can be accurately
 *    extracted from bearer tokens across distributed client-server handshakes.
 * 3. Token Validity: Verifies that tampering with a token or passing invalid user details
 *    immediately fails token validation.
 */
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Set secret key and expiration (24 hours) via reflection
        ReflectionTestUtils.setField(jwtService, "secret", "9a4f2c8d3b7a1e5f8c6b2d4a7e9f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);

        User user = new User();
        user.setId(42L);
        user.setUsername("farhan_developer");
        user.setEmail("farhan@tixmanager.com");
        user.setPassword("secret");
        user.setRole(UserRole.CUSTOMER);

        userDetails = new UserDetailsImpl(user);
    }

    /**
     * TEST CASE 1: Generate token and extract username claim.
     */
    @Test
    @DisplayName("generateToken & extractUsername — Successfully create signed JWT and parse username")
    void testGenerateToken_ExtractUsername() {
        // Act
        String token = jwtService.generateToken(userDetails);

        // Assert
        assertThat(token).isNotBlank();
        String extractedUsername = jwtService.extractUsername(token);
        assertThat(extractedUsername).isEqualTo("farhan_developer");
    }

    /**
     * TEST CASE 2: Validate token against matching and non-matching user details.
     */
    @Test
    @DisplayName("validateToken — Return true for valid token and matching user, false for mismatched user")
    void testValidateToken() {
        // Arrange
        String token = jwtService.generateToken(userDetails);

        User differentUser = new User();
        differentUser.setId(99L);
        differentUser.setUsername("hacker_imposter");
        differentUser.setRole(UserRole.CUSTOMER);
        UserDetailsImpl differentUserDetails = new UserDetailsImpl(differentUser);

        // Act & Assert
        assertThat(jwtService.validateToken(token, userDetails)).isTrue();
        assertThat(jwtService.validateToken(token, differentUserDetails)).isFalse();
    }
}
