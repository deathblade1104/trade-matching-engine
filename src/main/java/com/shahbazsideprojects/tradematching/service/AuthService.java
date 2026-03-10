package com.shahbazsideprojects.tradematching.service;

import com.shahbazsideprojects.tradematching.dto.auth.LoginResponseDto;
import com.shahbazsideprojects.tradematching.dto.auth.SignupResponseDto;
import com.shahbazsideprojects.tradematching.dto.auth.LoginDto;
import com.shahbazsideprojects.tradematching.dto.auth.SignupDto;
import com.shahbazsideprojects.tradematching.entity.User;
import com.shahbazsideprojects.tradematching.exception.ResourceConflictException;
import com.shahbazsideprojects.tradematching.exception.UnauthorizedException;
import com.shahbazsideprojects.tradematching.mapper.UserMapper;
import com.shahbazsideprojects.tradematching.security.JwtUtil;
import com.shahbazsideprojects.tradematching.security.UserPrincipal;
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
