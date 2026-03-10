package com.sideprojects.tradematching.controller;

import com.sideprojects.tradematching.dto.MessageData;
import com.sideprojects.tradematching.dto.auth.*;
import com.sideprojects.tradematching.security.AuthContext;
import com.sideprojects.tradematching.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthContext authContext;

    @PostMapping("/signup")
    public ResponseEntity<MessageData<SignupResponseDto>> signup(@Valid @RequestBody SignupDto dto) {
        SignupResponseDto data = authService.signup(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                MessageData.<SignupResponseDto>builder().message("Signup Successfull").data(data).build());
    }

    @PostMapping("/login")
    public ResponseEntity<MessageData<LoginResponseDto>> login(@Valid @RequestBody LoginDto dto) {
        LoginResponseDto data = authService.login(dto);
        return ResponseEntity.ok(
                MessageData.<LoginResponseDto>builder().message("Login Successfull").data(data).build());
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageData<Void>> logout(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : null;
        authService.logout(token, authContext.getPrincipal());
        return ResponseEntity.ok(
                MessageData.<Void>builder().message("Logout Successfull").data(null).build());
    }
}
