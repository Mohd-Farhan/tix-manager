package com.support.security;

import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.support.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        log.debug("Generating JWT token for user: {}", userDetails.getUsername());
        Map<String, Object> claims = extraClaims != null ? new HashMap<>(extraClaims) : new HashMap<>();
        if (!claims.containsKey("iat_ms")) {
            claims.put("iat_ms", System.currentTimeMillis());
        }
        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            if (!username.equals(userDetails.getUsername()) || isTokenExpired(token)) {
                log.warn("JWT token invalid or expired for user: {}", userDetails.getUsername());
                return false;
            }

            if (userDetails instanceof UserDetailsImpl udi) {
                User u = udi.getUser();
                if (u.isDeleted()) {
                    log.warn("JWT token rejected: user account is soft-deleted: {}", u.getUsername());
                    return false;
                }
                Long iatMs = extractClaim(token, claims -> claims.get("iat_ms", Long.class));
                long tokenIssuedMs = iatMs != null ? iatMs : (extractIssuedAt(token) != null ? extractIssuedAt(token).getTime() : 0L);

                if (u.getPasswordChangedAt() != null) {
                    long pwdChangedMs = u.getPasswordChangedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    if (tokenIssuedMs < pwdChangedMs) {
                        log.warn("JWT token revoked: issued before password change for user: {}", u.getUsername());
                        return false;
                    }
                }
                if (u.getLastLogoutAt() != null) {
                    long lastLogoutMs = u.getLastLogoutAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    if (tokenIssuedMs < lastLogoutMs) {
                        log.warn("JWT token revoked: issued before last logout for user: {}", u.getUsername());
                        return false;
                    }
                }
            }

            return true;
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public Date extractIssuedAt(String token) {
        return extractAllClaims(token).getIssuedAt();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
