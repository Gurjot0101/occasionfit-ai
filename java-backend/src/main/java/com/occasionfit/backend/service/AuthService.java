package com.occasionfit.backend.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public interface AuthService {
    Map<String, String> handleGoogleLogin(String idTokenString);

    Map<String, String> refreshToken(String refreshToken);

    void logout(String email);
}
