package com.xueren.service;

import com.xueren.common.BusinessException;
import com.xueren.config.JwtProperties;
import com.xueren.dto.*;
import com.xueren.entity.User;
import com.xueren.entity.UserToken;
import com.xueren.repository.UserRepository;
import com.xueren.repository.UserTokenRepository;
import com.xueren.security.JwtUtil;
import com.xueren.security.LoginRateLimiter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserTokenRepository userTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final UserService userService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final LoginRateLimiter loginRateLimiter = new LoginRateLimiter();

    public AuthService(UserRepository userRepository,
                       UserTokenRepository userTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       JwtProperties jwtProperties,
                       UserService userService) {
        this.userRepository = userRepository;
        this.userTokenRepository = userTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
        this.userService = userService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BusinessException("请输入用户名");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new BusinessException("密码长度至少6位");
        }
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new BusinessException("该用户名已被注册，请换一个");
        }
        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null && !request.getNickname().isBlank()
                ? request.getNickname().trim() : request.getUsername().trim());
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BusinessException("请输入用户名");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException("请输入密码");
        }

        String username = request.getUsername().trim();
        // 限流检查：连续失败5次锁定15分钟
        if (loginRateLimiter.isLocked(username)) {
            throw new BusinessException("登录失败次数过多，请15分钟后再试");
        }

        // 统一错误消息，防止用户枚举
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginRateLimiter.recordFailure(username);
            throw new BusinessException("用户名或密码错误");
        }

        // 登录成功，清除失败记录
        loginRateLimiter.clear(username);
        user.setLastOnlineAt(LocalDateTime.now());
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(Long userId) {
        userTokenRepository.deleteByUserId(userId);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String hash = sha256(request.getRefreshToken());
        UserToken token = userTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException("refreshToken 无效"));
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("refreshToken 已过期");
        }
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BusinessException("用户不存在"));
        userTokenRepository.delete(token);
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtil.createAccessToken(user.getId(), user.getUsername());
        String refreshToken = createRefreshToken(user.getId());
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userService.toVOWithOnline(user))
                .build();
    }

    private String createRefreshToken(Long userId) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        UserToken userToken = new UserToken();
        userToken.setUserId(userId);
        userToken.setTokenHash(sha256(refreshToken));
        userToken.setExpiresAt(LocalDateTime.now().plusDays(jwtProperties.getRefreshExpireDays()));
        userTokenRepository.save(userToken);
        return refreshToken;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new BusinessException("Token 生成失败");
        }
    }
}
