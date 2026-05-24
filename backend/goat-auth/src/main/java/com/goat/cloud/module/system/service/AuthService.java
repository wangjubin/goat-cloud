package com.goat.cloud.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.goat.cloud.common.enums.CommonStatus;
import com.goat.cloud.common.enums.DataScope;
import com.goat.cloud.common.exception.BusinessException;
import com.goat.cloud.framework.config.SecurityProperties;
import com.goat.cloud.framework.security.CurrentUserHolder;
import com.goat.cloud.framework.security.JwtTokenProvider;
import com.goat.cloud.framework.security.LoginSession;
import com.goat.cloud.framework.security.SessionService;
import com.goat.cloud.framework.security.TokenPair;
import com.goat.cloud.module.system.entity.SysLoginLog;
import com.goat.cloud.module.system.entity.SysRole;
import com.goat.cloud.module.system.entity.SysUser;
import com.goat.cloud.module.system.mapper.SysLoginLogMapper;
import com.goat.cloud.module.system.mapper.SysUserMapper;
import com.goat.cloud.module.system.model.request.LoginRequest;
import com.goat.cloud.module.system.model.vo.AuthUserVO;
import com.goat.cloud.module.system.model.vo.LoginResponseVO;
import com.goat.cloud.module.system.model.vo.ProfileVO;
import com.goat.cloud.module.system.service.MenuService;
import com.goat.cloud.module.system.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author wangjubin
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final RoleService roleService;
    private final MenuService menuService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SessionService sessionService;
    private final SysLoginLogMapper sysLoginLogMapper;
    private final SecurityProperties securityProperties;
    private final StringRedisTemplate stringRedisTemplate;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 10;

    public LoginResponseVO login(LoginRequest request, HttpServletRequest servletRequest) {
        // 登录限流：检查账号是否被临时锁定
        String lockKey = "goat:login:lock:" + request.getUsername();
        String locked = stringRedisTemplate.opsForValue().get(lockKey);
        if (locked != null) {
            throw new BusinessException(4001, "账号已锁定，请" + LOCKOUT_MINUTES + "分钟后重试");
        }

        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())
                .last("limit 1"));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // 递增失败计数
            String rateLimitKey = "goat:login:rate:" + request.getUsername();
            Long attempts = stringRedisTemplate.opsForValue().increment(rateLimitKey);
            if (attempts != null && attempts == 1) {
                stringRedisTemplate.expire(rateLimitKey, LOCKOUT_MINUTES, TimeUnit.MINUTES);
            }
            if (attempts != null && attempts >= MAX_LOGIN_ATTEMPTS) {
                stringRedisTemplate.opsForValue().set(lockKey, "1", LOCKOUT_MINUTES, TimeUnit.MINUTES);
                saveLoginLog(request.getUsername(), false, servletRequest, "连续失败" + MAX_LOGIN_ATTEMPTS + "次，账号锁定" + LOCKOUT_MINUTES + "分钟");
                throw new BusinessException(4001, "连续失败" + MAX_LOGIN_ATTEMPTS + "次，账号锁定" + LOCKOUT_MINUTES + "分钟");
            }
            saveLoginLog(request.getUsername(), false, servletRequest, "Username or password is incorrect");
            throw new BusinessException(4002, "Username or password is incorrect");
        }
        if (user.getStatus() == CommonStatus.DISABLED) {
            saveLoginLog(request.getUsername(), false, servletRequest, "Account is disabled");
            throw new BusinessException(4003, "Account is disabled");
        }

        // 登录成功，清除失败计数
        stringRedisTemplate.delete("goat:login:rate:" + request.getUsername());

        List<SysRole> roles = roleService.listByUserId(user.getUserId());
        if (roles.isEmpty()) {
            saveLoginLog(request.getUsername(), false, servletRequest, "Account has no roles");
            throw new BusinessException(4004, "Account has no roles");
        }

        LoginSession session = buildSession(user, roles);
        sessionService.saveSession(session);
        TokenPair tokenPair = new TokenPair(
                jwtTokenProvider.createAccessToken(user.getUserId(), session.getSessionId()),
                jwtTokenProvider.createRefreshToken(user.getUserId(), session.getSessionId())
        );

        saveLoginLog(request.getUsername(), true, servletRequest, "Login success");
        return LoginResponseVO.builder()
                .accessToken(tokenPair.getAccessToken())
                .refreshToken(tokenPair.getRefreshToken())
                .profile(buildProfile(session))
                .build();
    }

    public LoginResponseVO refresh(String refreshToken) {
        LoginSession session = sessionService.validateRefreshToken(refreshToken);
        session.setLastAccessTime(LocalDateTime.now());
        sessionService.touchSession(session);
        return LoginResponseVO.builder()
                .accessToken(jwtTokenProvider.createAccessToken(session.getUserId(), session.getSessionId()))
                .refreshToken(jwtTokenProvider.createRefreshToken(session.getUserId(), session.getSessionId()))
                .profile(buildProfile(session))
                .build();
    }

    public void logout() {
        LoginSession session = CurrentUserHolder.require();
        sessionService.destroySession(session.getUserId());
    }

    public ProfileVO profile() {
        return buildProfile(CurrentUserHolder.require());
    }

    private LoginSession buildSession(SysUser user, List<SysRole> roles) {
        DataScope scope = roles.stream()
                .map(SysRole::getDataScope)
                .min(Comparator.comparingInt(this::rankScope))
                .orElse(DataScope.SELF);
        List<Long> roleIds = roles.stream().map(SysRole::getRoleId).toList();
        return LoginSession.builder()
                .sessionId(UUID.randomUUID().toString())
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .deptId(user.getDeptId())
                .superAdmin(Boolean.TRUE.equals(user.getSuperAdmin()))
                .status(user.getStatus())
                .roleIds(roleIds)
                .roleCodes(roles.stream().map(SysRole::getRoleCode).toList())
                .permissions(menuService.listPermissionsByRoleIds(roleIds, Boolean.TRUE.equals(user.getSuperAdmin())))
                .dataScope(scope)
                .customDeptIds(roleService.listCustomDeptIds(roleIds, scope))
                .loginTime(LocalDateTime.now())
                .lastAccessTime(LocalDateTime.now())
                .build();
    }

    private ProfileVO buildProfile(LoginSession session) {
        return ProfileVO.builder()
                .user(AuthUserVO.builder()
                        .userId(session.getUserId())
                        .username(session.getUsername())
                        .nickname(session.getNickname())
                        .deptId(session.getDeptId())
                        .build())
                .roleCodes(session.getRoleCodes())
                .permissions(session.getPermissions())
                .routes(menuService.buildRoutesForSession(session))
                .build();
    }

    private void saveLoginLog(String username, boolean success, HttpServletRequest request, String message) {
        SysLoginLog log = new SysLoginLog();
        log.setUsername(username);
        log.setSuccess(success);
        log.setIpAddress(request.getRemoteAddr());
        log.setUserAgent(request.getHeader("User-Agent"));
        log.setMessage(message);
        log.setLoginTime(LocalDateTime.now());
        sysLoginLogMapper.insert(log);
    }

    private int rankScope(DataScope scope) {
        return switch (scope) {
            case ALL -> 0;
            case CUSTOM -> 1;
            case DEPT_AND_CHILD -> 2;
            case DEPT -> 3;
            case SELF -> 4;
        };
    }
}
