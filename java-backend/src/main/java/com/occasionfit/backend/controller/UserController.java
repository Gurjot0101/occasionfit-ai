package com.occasionfit.backend.controller;


import com.occasionfit.backend.dto.res.UserResponse;
import com.occasionfit.backend.model.User;
import com.occasionfit.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Log4j2
public class UserController {
    private final UserService userService;
    private final ModelMapper modelMapper = new ModelMapper();

    @GetMapping("/me")
    public ResponseEntity<?> getMe(@RequestHeader("Authorization") String authHeader) {
        try {
            String accessToken = authHeader.replace("Bearer ", "");
            User user = userService.getUserFromToken(accessToken);
            UserResponse userResponse = modelMapper.map(user, UserResponse.class);
            return ResponseEntity.ok(userResponse);
        } catch (Exception e) {
            log.error("/api/user/me error - error - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUser(@RequestHeader("Authorization") String authHeader) {
        try {
            String accessToken = authHeader.replace("Bearer ", "");
            userService.deleteUser(accessToken);
            return ResponseEntity.ok(Map.of("message", "User deleted"));
        } catch (Exception e) {
            log.error("/delete/user - error - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
    }
}