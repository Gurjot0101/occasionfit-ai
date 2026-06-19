package com.occasionfit.backend.repository;

import com.occasionfit.backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    void deleteByEmail(String email);
}

