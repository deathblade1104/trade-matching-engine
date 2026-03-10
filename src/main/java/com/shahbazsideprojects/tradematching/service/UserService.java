package com.shahbazsideprojects.tradematching.service;

import com.shahbazsideprojects.tradematching.dto.user.UserInfoResponseDto;
import com.shahbazsideprojects.tradematching.entity.User;
import com.shahbazsideprojects.tradematching.mapper.UserMapper;
import com.shahbazsideprojects.tradematching.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public User createUser(String name, String email, String passwordHash) {
        User user = User.builder()
                .name(name)
                .email(email)
                .passwordHash(passwordHash)
                .build();
        return userRepository.save(user);
    }

    public UserInfoResponseDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.shahbazsideprojects.tradematching.exception.ResourceNotFoundException("User not found"));
        return userMapper.toUserInfoResponseDto(user);
    }
}
