package com.w_s_backend.w_s.controllers;

import com.w_s_backend.w_s.DTOs.LoginDTO;
import com.w_s_backend.w_s.DTOs.UserRegistrationDTO;
import com.w_s_backend.w_s.models.User;

import org.springframework.ui.Model;

import com.w_s_backend.w_s.Services.UserService;
import lombok.AllArgsConstructor;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {
    
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    
    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@RequestBody UserRegistrationDTO registrationDTO) {
        User createdUser = userService.createUser(registrationDTO);
        return ResponseEntity.ok(createdUser);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDTO) {
        User user = userService.findByUsername(loginDTO.username);

        if(user == null || !passwordEncoder.matches(loginDTO.password, user.getPassword())){
            return ResponseEntity.status(401).body(Map.of("message", "Неверный логин или пароль"));
        }

        return ResponseEntity.ok(Map.of(
        "message", "Вход выполнен",
        "userId", user.getId(),
        "username", user.getUsername()
    ));
    }
    

}
