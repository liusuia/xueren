package com.xueren.service;

import com.xueren.common.BusinessException;
import com.xueren.dto.UpdateProfileRequest;
import com.xueren.dto.UserVO;
import com.xueren.entity.User;
import com.xueren.netty.ChannelManager;
import com.xueren.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ChannelManager channelManager;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final SecureRandom random = new SecureRandom();

    public UserService(UserRepository userRepository,
                       ChannelManager channelManager,
                       PasswordEncoder passwordEncoder,
                       JdbcTemplate jdbc,
                       MailService mailService) {
        this.userRepository = userRepository;
        this.channelManager = channelManager;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
        this.mailService = mailService;
    }

    public UserVO getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        return toVOWithOnline(user);
    }

    public List<UserVO> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return userRepository.findByUsernameContainingOrNicknameContaining(keyword, keyword)
                .stream()
                .map(this::toVOWithOnline)
                .toList();
    }

    public User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    @Transactional
    public UserVO updateAvatar(Long userId, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        user.setAvatar(avatarUrl);
        userRepository.save(user);
        return toVOWithOnline(user);
    }

    @Transactional
    public UserVO updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        if (request.getUsername() != null && !request.getUsername().isBlank()
                && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new BusinessException("用户名已被占用");
            }
            user.setUsername(request.getUsername());
        }
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getBirthday() != null) {
            user.setBirthday(request.getBirthday());
        }
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getOldPassword() == null || !passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                throw new BusinessException("原密码错误");
            }
            if (request.getNewPassword().length() < 6) {
                throw new BusinessException("新密码长度至少6位");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        userRepository.save(user);
        return toVOWithOnline(user);
    }

    public String sendEmailChangeCode(Long userId, String newEmail) {
        if (newEmail == null || newEmail.isBlank()) throw new BusinessException("邮箱不能为空");
        if (userRepository.findFirstByEmail(newEmail).isPresent())
            throw new BusinessException("该邮箱已被使用");
        String code = String.format("%06d", random.nextInt(1000000));
        jdbc.update("INSERT INTO password_reset (email, code, expires_at) VALUES (?, ?, ?)",
                newEmail, code, LocalDateTime.now().plusMinutes(10));
        mailService.sendResetCode(newEmail, code);
        return "验证码已发送至新邮箱";
    }

    @Transactional
    public UserVO changeEmail(Long userId, String newEmail, String code) {
        if (newEmail == null || newEmail.isBlank() || code == null || code.isBlank())
            throw new BusinessException("邮箱和验证码不能为空");
        var row = jdbc.queryForMap(
            "SELECT id FROM password_reset WHERE email=? AND code=? AND used=0 AND expires_at>? ORDER BY id DESC LIMIT 1",
            newEmail, code, LocalDateTime.now());
        if (row == null || row.isEmpty()) throw new BusinessException("验证码无效或已过期");
        jdbc.update("UPDATE password_reset SET used=1 WHERE id=?", row.get("id"));
        if (userRepository.findFirstByEmail(newEmail).isPresent())
            throw new BusinessException("该邮箱已被使用");
        User user = requireUser(userId);
        user.setEmail(newEmail);
        userRepository.save(user);
        return toVOWithOnline(user);
    }

    @Transactional
    public void changePassword(Long userId, String oldPwd, String newPwd) {
        if (newPwd == null || newPwd.length() < 6) throw new BusinessException("新密码至少6位");
        User user = requireUser(userId);
        if (!passwordEncoder.matches(oldPwd, user.getPassword())) throw new BusinessException("原密码错误");
        user.setPassword(passwordEncoder.encode(newPwd));
        userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(Long userId) {
        // 清理关联数据
        jdbc.update("DELETE FROM friend WHERE user_id=? OR friend_id=?", userId, userId);
        jdbc.update("DELETE FROM conversation WHERE user_id=?", userId);
        jdbc.update("DELETE FROM message_read WHERE user_id=?", userId);
        jdbc.update("DELETE FROM message_hidden WHERE user_id=?", userId);
        jdbc.update("DELETE FROM user_token WHERE user_id=?", userId);
        userRepository.deleteById(userId);
    }

    @Transactional
    public UserVO changeUsername(Long userId, String newUsername) {
        if (newUsername == null || newUsername.isBlank() || newUsername.length() < 2) {
            throw new BusinessException("轻语ID长度至少2位");
        }
        User user = requireUser(userId);
        // 一年内只能改一次
        if (user.getUsernameChangedAt() != null &&
                user.getUsernameChangedAt().plusYears(1).isAfter(LocalDateTime.now())) {
            throw new BusinessException("轻语ID一年内只能修改一次，请于 " +
                user.getUsernameChangedAt().plusYears(1).toLocalDate() + " 后再试");
        }
        if (userRepository.existsByUsername(newUsername.trim())) {
            throw new BusinessException("该ID已被使用");
        }
        user.setUsername(newUsername.trim());
        user.setUsernameChangedAt(LocalDateTime.now());
        userRepository.save(user);
        return toVOWithOnline(user);
    }

    public static UserVO toVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .phone(user.getPhone())
                .birthday(user.getBirthday())
                .lastOnlineAt(user.getLastOnlineAt())
                .createdAt(user.getCreatedAt())
                .isOnline(false)
                .build();
    }

    public UserVO toVOWithOnline(User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .phone(user.getPhone())
                .birthday(user.getBirthday())
                .lastOnlineAt(user.getLastOnlineAt())
                .createdAt(user.getCreatedAt())
                .isOnline(channelManager.isOnline(user.getId()))
                .build();
    }
}
