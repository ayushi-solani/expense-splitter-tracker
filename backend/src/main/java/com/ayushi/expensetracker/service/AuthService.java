package com.ayushi.expensetracker.service;

import com.ayushi.expensetracker.config.JwtUtil;
import com.ayushi.expensetracker.dto.AuthResponse;
import com.ayushi.expensetracker.dto.LoginRequest;
import com.ayushi.expensetracker.dto.RegisterRequest;
import com.ayushi.expensetracker.entity.User;
import com.ayushi.expensetracker.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service

public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists!");
        }

        User user =  new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getName()
        );
        User saved = userRepository.save(user);

        String token = jwtUtil.generateToken(saved.getEmail(), saved.getId());
        return new AuthResponse(saved.getId(), saved.getEmail(), saved.getName(), "Registration successful", token);
    }

    public AuthResponse login(LoginRequest request) {
        User use = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password!"));
        if(!passwordEncoder.matches(request.getPassword(), use.getPasswordHash())){
            throw new IllegalArgumentException("Invalid password!");
        }
        String token = jwtUtil.generateToken(use.getEmail(), use.getId());
        return new AuthResponse(use.getId(), use.getEmail(), use.getName(), "Login successful", token);
    }
}
