package com.occasionfit.backend.repository;

import com.occasionfit.backend.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MessageRepository extends MongoRepository<Message, String> {
    List<Message> findByThreadIdOrderByTimestampAsc(String threadId);
    void deleteByThreadId(String threadId);
    List<Message> findTop10ByThreadIdOrderByTimestampDesc(String threadId);
}
