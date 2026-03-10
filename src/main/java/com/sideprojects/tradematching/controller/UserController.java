package com.sideprojects.tradematching.controller;

import com.sideprojects.tradematching.dto.MessageData;
import com.sideprojects.tradematching.dto.user.UserInfoResponseDto;
import com.sideprojects.tradematching.security.AuthContext;
import com.sideprojects.tradematching.service.UserService;
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
