package com.occasionfit.backend.service;

import com.occasionfit.backend.model.User;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
    User getUserFromToken(String token);

    void deleteUser(String token);
}
