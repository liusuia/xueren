package com.xueren.service;

import com.xueren.common.BusinessException;
import com.xueren.config.JwtProperties;
import com.xueren.dto.*;
import com.xueren.entity.User;
import com.xueren.entity.UserToken;
import com.xueren.entity.Friend;
import com.xueren.repository.FriendRepository;
import com.xueren.repository.UserRepository;
import com.xueren.repository.UserTokenRepository;
import com.xueren.security.JwtUtil;
import com.xueren.security.LoginRateLimiter;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final FriendRepository friendRepository;
    private final JdbcTemplate jdbcTemplate;
    private final MailService mailService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final LoginRateLimiter loginRateLimiter = new LoginRateLimiter();

    public AuthService(UserRepository userRepository,
                       UserTokenRepository userTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       JwtProperties jwtProperties,
                       UserService userService,
                       FriendRepository friendRepository,
                       JdbcTemplate jdbcTemplate,
                       MailService mailService) {
        this.userRepository = userRepository;
        this.userTokenRepository = userTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
        this.userService = userService;
        this.friendRepository = friendRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.mailService = mailService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new BusinessException("密码长度至少6位");
        }
        User user = new User();
        // 自动生成唯一轻语号
        String autoId = generateQingyuId();
        user.setUsername(autoId);
        user.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null && !request.getNickname().isBlank()
                ? request.getNickname().trim() : autoId);
        userRepository.save(user);
        createFileHelperFriendship(user.getId());
        return buildAuthResponse(user);
    }

    private void createFileHelperFriendship(Long userId) {
        Long fid = 1L; if (userId.equals(fid)) return;
        if (friendRepository.findByUserIdAndFriendId(userId, fid).isEmpty()) {
            Friend f = new Friend(); f.setUserId(userId); f.setFriendId(fid);
            f.setRequesterId(fid); f.setStatus(1); friendRepository.save(f);
        }
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BusinessException("请输入用户名");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException("请输入密码");
        }

        String account = request.getUsername().trim(); // 用户名或邮箱
        if (loginRateLimiter.isLocked(account)) {
            throw new BusinessException("登录失败次数过多，请15分钟后再试");
        }

        // 支持用户名或邮箱登录
        User user;
        if (account.contains("@")) {
            user = userRepository.findFirstByEmail(account).orElse(null);
        } else {
            user = userRepository.findByUsername(account).orElse(null);
        }
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginRateLimiter.recordFailure(account);
            throw new BusinessException("用户名或密码错误");
        }

        loginRateLimiter.clear(account);
        user.setLastOnlineAt(LocalDateTime.now());
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public String forgotPassword(String email) {
        User user = userRepository.findFirstByEmail(email)
                .orElseThrow(() -> new BusinessException("该邮箱未注册"));
        String code = String.format("%06d", secureRandom.nextInt(1000000));
        jdbcTemplate.update(
            "INSERT INTO password_reset (email, code, expires_at) VALUES (?, ?, ?)",
            email, code, LocalDateTime.now().plusMinutes(10));
        mailService.sendResetCode(email, code);
        return code;
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("密码长度至少6位");
        }
        var row = jdbcTemplate.queryForMap(
            "SELECT id FROM password_reset WHERE email=? AND code=? AND used=0 AND expires_at>? ORDER BY id DESC LIMIT 1",
            email, code, LocalDateTime.now());
        if (row == null || row.isEmpty()) {
            throw new BusinessException("验证码无效或已过期");
        }
        jdbcTemplate.update("UPDATE password_reset SET used=1 WHERE id=?", row.get("id"));
        User user = userRepository.findFirstByEmail(email).get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
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

    private String generateQingyuId() {
        String id;
        do {
            byte[] bytes = new byte[5];
            secureRandom.nextBytes(bytes);
            id = "qy_" + HexFormat.of().formatHex(bytes);
        } while (userRepository.existsByUsername(id));
        return id;
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
