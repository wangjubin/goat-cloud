package com.goat.cloud.framework.security;

import com.goat.cloud.common.exception.BusinessException;
import com.goat.cloud.framework.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * @author wangjubin
 */
@Service
@RequiredArgsConstructor
public class SessionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityProperties securityProperties;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginSession saveSession(LoginSession session) {
        session.setRefreshExpireAt(LocalDateTime.now().plusDays(securityProperties.getRefreshTokenDays()));
        redisTemplate.opsForValue().set(buildSessionKey(session.getUserId()), session,
                Duration.ofDays(securityProperties.getRefreshTokenDays()));
        return session;
    }

    public LoginSession loadSession(Long userId) {
        Object value = redisTemplate.opsForValue().get(buildSessionKey(userId));
        return value instanceof LoginSession session ? session : null;
    }

    public void destroySession(Long userId) {
        redisTemplate.delete(buildSessionKey(userId));
    }

    public LoginSession validateRefreshToken(String refreshToken) {
        Claims claims = jwtTokenProvider.parseRefreshToken(refreshToken);
        Long userId = claims.get("userId", Long.class);
        String sessionId = claims.get("sessionId", String.class);
        LoginSession session = loadSession(userId);
        if (session == null || !sessionId.equals(session.getSessionId())) {
            throw new BusinessException(4011, "Refresh token expired");
        }
        touchSession(session);
        return session;
    }

    public void touchSession(LoginSession session) {
        redisTemplate.opsForValue().set(buildSessionKey(session.getUserId()), session,
                Duration.ofDays(securityProperties.getRefreshTokenDays()));
    }

    private String buildSessionKey(Long userId) {
        return "goat:session:user:" + userId;
    }
}
