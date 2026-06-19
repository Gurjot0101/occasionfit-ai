package com.occasionfit.backend.service;

import com.occasionfit.backend.model.Message;
import com.occasionfit.backend.model.enums.Sender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface MessageService {

    Message saveMessage(String threadId, String text, Sender sender, List<String> images, int tokens);

    List<Message> findAllByThreadId(String threadId);

    void deleteMessages(String threadId);

    List<Message> getRecentMessages(String threadId, int summaryTokens);
}
