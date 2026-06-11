package com.agent.server.service;

import com.agent.server.model.dto.AuthResponse;
import com.agent.server.model.dto.LoginRequest;
import com.agent.server.model.dto.RegisterRequest;
import com.agent.server.model.entity.SysUser;
import com.agent.server.model.entity.SysUserRole;
import com.agent.server.repository.SysUserRepository;
import com.agent.server.repository.SysUserRoleRepository;
import com.agent.server.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserRepository userRepository;
    private final SysUserRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setAuthProvider("LOCAL");
        user.setLastLoginAt(LocalDateTime.now());
        user = userRepository.save(user);

        // Assign USER role
        SysUserRole role = new SysUserRole();
        role.setUserId(user.getId());
        role.setRole("USER");
        roleRepository.save(role);

        return buildAuthResponse(user, List.of("USER"));
    }

    public AuthResponse login(LoginRequest request) {
        SysUser user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (user.getStatus() != 1) {
            throw new RuntimeException("账号已被禁用");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        List<String> roles = roleRepository.findByUserId(user.getId())
                .stream().map(SysUserRole::getRole).toList();

        return buildAuthResponse(user, roles);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String username = tokenProvider.getUsernameFromToken(refreshToken);
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<String> roles = roleRepository.findByUserId(user.getId())
                .stream().map(SysUserRole::getRole).toList();

        return buildAuthResponse(user, roles);
    }

    public SysUser getCurrentUser(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    private AuthResponse buildAuthResponse(SysUser user, List<String> roles) {
        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId(), user.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(tokenProvider.getExpirationMs())
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .nickname(user.getNickname())
                        .avatarUrl(user.getAvatarUrl())
                        .role(roles.isEmpty() ? "USER" : roles.get(0))
                        .build())
                .build();
    }
}
