package com.minicrm.controller;

import com.minicrm.model.User;
import com.minicrm.repository.mongo.db.UserRepository;
import com.minicrm.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    // ── POST /api/auth/register ─────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest body) {
        if (body.email() == null || body.email().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        if (body.password() == null || body.password().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters"));
        }
        if (userRepository.existsByEmail(body.email().toLowerCase())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "An account with this email already exists"));
        }

        User user = User.builder()
                .email(body.email().toLowerCase().strip())
                .name(body.name() != null ? body.name().strip() : body.email().split("@")[0])
                .passwordHash(passwordEncoder.encode(body.password()))
                .role("USER")
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse(user, token));
    }

    // ── POST /api/auth/login ────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body) {
        if (body.email() == null || body.password() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }

        Optional<User> userOpt = userRepository.findByEmail(body.email().toLowerCase());
        if (userOpt.isEmpty() || !passwordEncoder.matches(body.password(), userOpt.get().getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        }

        User user = userOpt.get();
        String token = jwtService.generateToken(user.getEmail());
        return ResponseEntity.ok(authResponse(user, token));
    }

    // ── Shared response shape ────────────────────────────────────────────

    private Map<String, Object> authResponse(User user, String token) {
        return Map.of(
                "token", token,
                "user", Map.of(
                        "id",    user.getId(),
                        "name",  user.getName(),
                        "email", user.getEmail(),
                        "role",  user.getRole()
                )
        );
    }

    // ── Request DTOs (Java 16+ records) ──────────────────────────────────

    record RegisterRequest(String name, String email, String password) {}
    record LoginRequest(String email, String password) {}
}
