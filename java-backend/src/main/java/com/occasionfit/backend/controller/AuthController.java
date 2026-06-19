package com.occasionfit.backend.controller;

import com.occasionfit.backend.service.AuthService;
import com.occasionfit.backend.security.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Log4j2
public class AuthController {

    private final AuthService authServiceImpl;
    private final JwtService jwtService;

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> body) {
        try {
            String idToken = body.get("idToken");
            if (idToken == null || idToken.isBlank()) return ResponseEntity.badRequest().body("idToken is required");
            return ResponseEntity.ok(authServiceImpl.handleGoogleLogin(idToken));
        } catch (Exception e) {
            log.error("/api/auth/google error - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        try {
            String refreshToken = body.get("refreshToken");
            if (refreshToken == null || refreshToken.isBlank())
                return ResponseEntity.badRequest().body("refreshToken is required");
            Map<String, String> tokens = authServiceImpl.refreshToken(refreshToken);
            return ResponseEntity.ok(tokens);
        } catch (ExpiredJwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token expired");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> body) {
        String email = jwtService.extractEmail(body.get("refreshToken"));
        authServiceImpl.logout(email);
        return ResponseEntity.ok("Logged out");
    }
}
