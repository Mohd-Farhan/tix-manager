package com.support.service;

import com.support.entity.RefreshToken;
import com.support.entity.User;
import com.support.entity.UserRole;
import com.support.exception.RefreshTokenException;
import com.support.repository.RefreshTokenRepository;
import com.support.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ==============================================================================================
 * UNIT TEST SUITE: RefreshTokenServiceTest
 * ==============================================================================================
 * 
 * Validates:
 * 1. Cryptographic token issuance with 7-day expiration.
 * 2. Token rotation burning old token and returning new token.
 * 3. OWASP Token Reuse Breach Detection: presenting revoked token invalidates all user sessions.
 * 4. Expired token rejection.
 * 5. Soft-deleted / deactivated user rejection.
 * 6. Real-time session revocation via lastLogoutAt.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    private RefreshTokenService refreshTokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, userRepository, 604800000L);

        testUser = new User();
        testUser.setId(100L);
        testUser.setUsername("testuser");
        testUser.setEmail("testuser@example.com");
        testUser.setRole(UserRole.CUSTOMER);
        testUser.setDeleted(false);
    }

    @Test
    @DisplayName("createRefreshToken — Successfully creates unrevoked 7-day refresh token")
    void testCreateRefreshToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshToken token = refreshTokenService.createRefreshToken(testUser);

        assertThat(token).isNotNull();
        assertThat(token.getToken()).isNotBlank();
        assertThat(token.getUser()).isEqualTo(testUser);
        assertThat(token.isRevoked()).isFalse();
        assertThat(token.getExpiryDate()).isAfter(Instant.now());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("rotateRefreshToken — Successfully rotates valid token into new token pair")
    void testRotateRefreshToken_Success() {
        RefreshToken existing = RefreshToken.builder()
                .id(1L)
                .token("old-token-val")
                .user(testUser)
                .expiryDate(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("old-token-val")).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshToken rotated = refreshTokenService.rotateRefreshToken("old-token-val");

        assertThat(rotated).isNotNull();
        assertThat(rotated.getToken()).isNotEqualTo("old-token-val");
        assertThat(existing.isRevoked()).isTrue();
        assertThat(existing.getReplacedByToken()).isEqualTo(rotated.getToken());
    }

    @Test
    @DisplayName("rotateRefreshToken — OWASP Breach Detection: Revoked token reuse revokes all sessions")
    void testRotateRefreshToken_BreachDetection() {
        RefreshToken revokedToken = RefreshToken.builder()
                .id(2L)
                .token("stolen-revoked-token")
                .user(testUser)
                .expiryDate(Instant.now().plusSeconds(3600))
                .revoked(true)
                .build();

        when(refreshTokenRepository.findByToken("stolen-revoked-token")).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("stolen-revoked-token"))
                .isInstanceOf(RefreshTokenException.class)
                .hasMessageContaining("previously revoked");

        // Verify breach defense: all user tokens revoked and lastLogoutAt updated
        verify(refreshTokenRepository).revokeAllUserTokens(testUser);
        verify(userRepository).save(testUser);
        assertThat(testUser.getLastLogoutAt()).isNotNull();
    }

    @Test
    @DisplayName("rotateRefreshToken — Expired token throws RefreshTokenException and marks revoked")
    void testRotateRefreshToken_Expired() {
        RefreshToken expiredToken = RefreshToken.builder()
                .id(3L)
                .token("expired-token")
                .user(testUser)
                .expiryDate(Instant.now().minusSeconds(60))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("expired-token"))
                .isInstanceOf(RefreshTokenException.class)
                .hasMessageContaining("expired");

        assertThat(expiredToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(expiredToken);
    }

    @Test
    @DisplayName("rotateRefreshToken — Deactivated user throws RefreshTokenException")
    void testRotateRefreshToken_DeactivatedUser() {
        testUser.setDeleted(true);
        RefreshToken token = RefreshToken.builder()
                .id(4L)
                .token("user-deleted-token")
                .user(testUser)
                .expiryDate(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("user-deleted-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("user-deleted-token"))
                .isInstanceOf(RefreshTokenException.class)
                .hasMessageContaining("deactivated");

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    @DisplayName("revokeAllUserTokens — Revokes tokens and updates user lastLogoutAt")
    void testRevokeAllUserTokens() {
        refreshTokenService.revokeAllUserTokens(testUser);

        verify(refreshTokenRepository).revokeAllUserTokens(testUser);
        verify(userRepository).save(testUser);
        assertThat(testUser.getLastLogoutAt()).isNotNull();
    }

    @Test
    @DisplayName("revokeToken — Single device logout revokes token and sets lastLogoutAt")
    void testRevokeToken() {
        RefreshToken token = RefreshToken.builder()
                .id(5L)
                .token("device-token")
                .user(testUser)
                .expiryDate(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("device-token")).thenReturn(Optional.of(token));

        refreshTokenService.revokeToken("device-token");

        assertThat(token.isRevoked()).isTrue();
        assertThat(testUser.getLastLogoutAt()).isNotNull();
        verify(refreshTokenRepository).save(token);
        verify(userRepository).save(testUser);
    }
}
