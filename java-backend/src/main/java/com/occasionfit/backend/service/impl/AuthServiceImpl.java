package com.occasionfit.backend.service.impl;


import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.occasionfit.backend.model.User;
import com.occasionfit.backend.repository.UserRepository;
import com.occasionfit.backend.service.AuthService;
import com.occasionfit.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class AuthServiceImpl implements AuthService {
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Value("${google.web.client.id}")
    private String WEB_CLIENT_ID;

    public Map<String, String> handleGoogleLogin(String idTokenString) {

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                new GsonFactory())
                .setAudience(Collections.singletonList(WEB_CLIENT_ID))
                .build();

        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(idTokenString);
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Token verification failed", e);
        }

        if (idToken == null) {
            throw new RuntimeException("Invalid Google Token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        User user = userRepository.findByEmail(payload.getEmail())
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(payload.getEmail());
                    newUser.setName((String) payload.get("name"));
                    newUser.setImage((String) payload.get("picture"));
                    log.info("New User Signed Up: {} - {}", newUser.getName(), newUser.getEmail());
                    return newUser;
                });

        String latestName = (String) payload.get("name");
        String latestImage = (String) payload.get("picture");

        if (!latestName.equals(user.getName()) || !latestImage.equals(user.getImage())) {
            user.setName(latestName);
            user.setImage(latestImage);
            log.info("User profile updated: {}", user.getEmail());
        }

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        user.setRefreshToken(refreshToken);

        log.info("User logged in: {}", user.getEmail());
        userRepository.save(user);

        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        );
    }

    public Map<String, String> refreshToken(String refreshToken) {
        String email = jwtService.extractEmail(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate against stored token (prevents reuse after rotation)
        if (!refreshToken.equals(user.getRefreshToken())) {
            // Token reuse detected — invalidate everything
            user.setRefreshToken(null);
            userRepository.save(user);
            throw new RuntimeException("Refresh token reuse detected");
        }

        // Generate new both tokens (rotation)
        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        // Save new refresh token, old one is now invalid
        user.setRefreshToken(newRefreshToken);
        userRepository.save(user);

        return Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken
        );
    }

    public void logout(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setRefreshToken(null);
            userRepository.save(user);
            log.info("User logged out: {}", user.getEmail());
        });
    }
}