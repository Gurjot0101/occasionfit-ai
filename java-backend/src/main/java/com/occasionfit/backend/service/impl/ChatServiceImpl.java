package com.occasionfit.backend.service.impl;

import com.occasionfit.backend.agent.AgentContext;
import com.occasionfit.backend.agent.AgentOrchestrator;
import com.occasionfit.backend.agent.AgentResult;
import com.occasionfit.backend.dto.req.MessageRequest;
import com.occasionfit.backend.dto.res.MessageResponse;
import com.occasionfit.backend.model.ChatThread;
import com.occasionfit.backend.model.Message;
import com.occasionfit.backend.model.enums.Sender;
import com.occasionfit.backend.service.ChatService;
import com.occasionfit.backend.service.ChatThreadService;
import com.occasionfit.backend.service.ContextService;
import com.occasionfit.backend.service.MessageService;
import com.occasionfit.backend.util.TokenEstimator;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Log4j2
public class ChatServiceImpl implements ChatService {
    private final MessageService messageService;
    private final ChatThreadService chatThreadService;
    private final ContextService contextServiceImpl;
    private final AgentOrchestrator agentOrchestrator;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public MessageResponse handleMessage(MessageRequest request, String email) {
        ChatThread thread;

        if (request.getThreadId() == null || request.getThreadId().isEmpty())
            thread = chatThreadService.createChatThread(email, "");
        else thread = chatThreadService.getThread(request.getThreadId());

        List<Message> recentMessages = messageService.getRecentMessages(
                thread.getId(),
                thread.getSummaryTokenCount()
        );

        AgentContext agentCtx = AgentContext.builder()
                .userMessage(request.getMessage())
                .threadContext(thread.getThreadContext())
                .base64Images(request.getImages())
                .recentMessages(recentMessages)
                .build();

        AgentResult agentResult = agentOrchestrator.run(agentCtx);
        String aiText = agentResult.getText();
        String aiImage = agentResult.getImage();

        int userMessageTokens = TokenEstimator.estimateTokens(request.getMessage());
        messageService.saveMessage(
                thread.getId(),
                request.getMessage(),
                Sender.USER,
                request.getImages() != null ? request.getImages() : Collections.emptyList(),
                userMessageTokens
        );

        int aiTokens = TokenEstimator.estimateTokens(aiText);
        Message aiMessage = messageService.saveMessage(
                thread.getId(),
                aiText,
                Sender.AI,
                aiImage != null ? List.of(aiImage) : Collections.emptyList(),
                aiTokens
        );

        if (shouldUpdateContext(thread)) executor.submit(() -> contextServiceImpl.updateContext(thread.getId()));
        return new MessageResponse(aiMessage, thread);
    }

    public List<ChatThread> getChatThreads(String email) {
        return chatThreadService.getThreads(email);
    }

    public void deleteChatThread(String threadId) {
        chatThreadService.deleteThread(threadId);
    }

    public List<Message> getMessages(String threadId) {
        return messageService.findAllByThreadId(threadId);
    }

    public void deleteMessages(String threadId) {
        messageService.deleteMessages(threadId);
    }

    private boolean shouldUpdateContext(ChatThread thread) {
        return thread.getMessageCount() - thread.getLastSummaryAtCount() >= 5
                || thread.getTotalTokens() > 3000;
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}