package com.occasionfit.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.occasionfit.backend.dto.req.MessageRequest;
import com.occasionfit.backend.dto.res.MessageResponse;
import com.occasionfit.backend.model.ChatThread;
import com.occasionfit.backend.model.Message;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ChatService {
    MessageResponse handleMessage(MessageRequest request, String email) throws JsonProcessingException;

    List<ChatThread> getChatThreads(String email);

    void deleteChatThread(String threadId);

    List<Message> getMessages(String threadId);

    void deleteMessages(String threadId);

}
