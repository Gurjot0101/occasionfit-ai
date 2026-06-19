package com.occasionfit.backend.repository;

import com.occasionfit.backend.model.ChatThread;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatThreadRepository
        extends MongoRepository<ChatThread, String>, ChatThreadRepositoryCustom {

    List<ChatThread> findByUserIdOrderByUpdatedAtDesc(String userId);
    List<ChatThread> findByCreatedAtBefore(LocalDateTime date);
}
