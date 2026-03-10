package com.shahbazsideprojects.tradematching.controller;

import com.shahbazsideprojects.tradematching.dto.MessageData;
import com.shahbazsideprojects.tradematching.dto.user.UserInfoResponseDto;
import com.shahbazsideprojects.tradematching.security.AuthContext;
import com.shahbazsideprojects.tradematching.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthContext authContext;

    @GetMapping("/info")
    public ResponseEntity<MessageData<UserInfoResponseDto>> getUserInfo() {
        UserInfoResponseDto data = userService.getUserById(authContext.getCurrentUserId());
        return ResponseEntity.ok(
                MessageData.<UserInfoResponseDto>builder()
                        .message("User Info fetched successfully")
                        .data(data)
                        .build());
    }
}
