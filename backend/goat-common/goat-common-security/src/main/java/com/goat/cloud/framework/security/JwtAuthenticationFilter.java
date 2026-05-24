package com.goat.cloud.framework.security;

import com.goat.cloud.common.api.ApiResponse;
import com.goat.cloud.common.enums.CommonStatus;
import com.goat.cloud.framework.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author wangjubin
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtTokenProvider jwtTokenProvider;
    private final SessionService sessionService;
    private final SecurityProperties securityProperties;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return PATH_MATCHER.match("/api/auth/login", path)
                || PATH_MATCHER.match("/api/auth/refresh", path)
                || PATH_MATCHER.match("/v3/api-docs/**", path)
                || PATH_MATCHER.match("/swagger-ui/**", path)
                || PATH_MATCHER.match("/actuator/health", path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtTokenProvider.parseAccessToken(token);
            Long userId = claims.get("userId", Long.class);
            String sessionId = claims.get("sessionId", String.class);
            LoginSession session = sessionService.loadSession(userId);

            if (session == null || !sessionId.equals(session.getSessionId()) || session.getStatus() == CommonStatus.DISABLED) {
                throw new SecurityException("Session expired");
            }

            session.setLastAccessTime(LocalDateTime.now());
            sessionService.touchSession(session);

            List<SimpleGrantedAuthority> authorities = session.getPermissions().stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    session, token, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            // Token 存在但解析失败 → 返回 401，不继续 filter chain
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            try {
                response.getWriter().write(objectMapper.writeValueAsString(
                        ApiResponse.fail(4010, "Invalid or expired token")));
            } catch (IOException ignored) {
            }
            return;
        }

        filterChain.doFilter(request, response);
    }
}
