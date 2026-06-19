package com.occasionfit.backend.service.impl;

import com.occasionfit.backend.model.User;
import com.occasionfit.backend.repository.UserRepository;
import com.occasionfit.backend.security.JwtService;
import com.occasionfit.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public User getUserFromToken(String token) {
        String email = jwtService.extractEmail(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found!"));
    }

    public void deleteUser(String token) {
        String email = jwtService.extractEmail(token);
        log.info("User deleted with email: {}", email);
        userRepository.deleteByEmail(email);
    }
}