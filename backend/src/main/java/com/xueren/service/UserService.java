package com.xueren.service;

import com.xueren.common.BusinessException;
import com.xueren.dto.UpdateProfileRequest;
import com.xueren.dto.UserVO;
import com.xueren.entity.User;
import com.xueren.netty.ChannelManager;
import com.xueren.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ChannelManager channelManager;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       ChannelManager channelManager,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.channelManager = channelManager;
        this.passwordEncoder = passwordEncoder;
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
