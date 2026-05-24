package com.goat.cloud.framework.security;

import com.goat.cloud.framework.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

/**
 * @author wangjubin
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final SecurityProperties securityProperties;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, String sessionId) {
        return securityProperties.getTokenPrefix() + createToken(userId, sessionId, TokenType.ACCESS,
                securityProperties.getAccessTokenMinutes(), ChronoUnit.MINUTES);
    }

    public String createRefreshToken(Long userId, String sessionId) {
        return securityProperties.getTokenPrefix() + createToken(userId, sessionId, TokenType.REFRESH,
                securityProperties.getRefreshTokenDays(), ChronoUnit.DAYS);
    }

    public Claims parseAccessToken(String token) {
        Claims claims = parseToken(token).getPayload();
        if (!TokenType.ACCESS.name().equals(claims.get("tokenType", String.class))) {
            throw new SecurityException("Invalid access token");
        }
        return claims;
    }

    public Claims parseRefreshToken(String token) {
        Claims claims = parseToken(token).getPayload();
        if (!TokenType.REFRESH.name().equals(claims.get("tokenType", String.class))) {
            throw new SecurityException("Invalid refresh token");
        }
        return claims;
    }

    private String createToken(Long userId, String sessionId, TokenType tokenType, long amount, ChronoUnit unit) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(Map.of(
                        "userId", userId,
                        "sessionId", sessionId,
                        "tokenType", tokenType.name()
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(amount, unit)))
                .signWith(secretKey)
                .compact();
    }

    private Jws<Claims> parseToken(String token) {
        String rawToken = token.startsWith(securityProperties.getTokenPrefix())
                ? token.substring(securityProperties.getTokenPrefix().length())
                : token;
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(rawToken);
    }
}
