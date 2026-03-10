package com.sideprojects.tradematching.service;

import com.sideprojects.tradematching.dto.auth.LoginResponseDto;
import com.sideprojects.tradematching.dto.auth.SignupResponseDto;
import com.sideprojects.tradematching.dto.auth.LoginDto;
import com.sideprojects.tradematching.dto.auth.SignupDto;
import com.sideprojects.tradematching.entity.User;
import com.sideprojects.tradematching.exception.ResourceConflictException;
import com.sideprojects.tradematching.exception.UnauthorizedException;
import com.sideprojects.tradematching.mapper.UserMapper;
import com.sideprojects.tradematching.security.JwtUtil;
import com.sideprojects.tradematching.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final CacheService cacheService;
    private final UserMapper userMapper;

    public SignupResponseDto signup(SignupDto dto) {
        if (userService.getUserByEmail(dto.getEmail()).isPresent()) {
            throw new ResourceConflictException("Email already in use");
        }
        String hash = passwordEncoder.encode(dto.getPassword());
        User user = userService.createUser(dto.getName(), dto.getEmail(), hash);
        return userMapper.toSignupResponseDto(user);
    }

    public LoginResponseDto login(LoginDto dto) {
        User user = userService.getUserByEmail(dto.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getName());
        return LoginResponseDto.builder()
                .accessToken(token)
                .build();
    }

    public void logout(String token, UserPrincipal principal) {
        if (principal == null) return;
        Date exp = jwtUtil.getExpiration(token);
        if (exp == null) return;
        long ttlMs = exp.getTime() - System.currentTimeMillis();
        if (ttlMs > 0) {
            String key = cacheService.getCacheKey("auth", "blacklist", token);
            cacheService.setValue(key, true, ttlMs);
        }
    }

    public boolean isBlacklisted(String token) {
        if (token == null || token.isEmpty()) return false;
        String key = cacheService.getCacheKey("auth", "blacklist", token);
        Object v = cacheService.getValue(key);
        return Boolean.TRUE.equals(v);
    }
}
