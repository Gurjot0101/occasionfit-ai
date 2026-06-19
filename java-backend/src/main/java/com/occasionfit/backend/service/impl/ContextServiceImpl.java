package com.occasionfit.backend.service.impl;

import com.occasionfit.backend.model.ChatThread;
import com.occasionfit.backend.model.Message;
import com.occasionfit.backend.model.enums.Sender;
import com.occasionfit.backend.repository.ChatThreadRepository;
import com.occasionfit.backend.service.ContextService;
import com.occasionfit.backend.service.MessageService;
import com.occasionfit.backend.ai.client.GeminiClient;
import com.occasionfit.backend.util.TokenEstimator;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class ContextServiceImpl implements ContextService {

    private final MessageService messageServiceImpl;
    private final ChatThreadRepository chatThreadRepository;
    private final GeminiClient geminiClient;

    private static final int SUMMARY_TOKEN_LIMIT = 800;
    private static final int MAX_CONTEXT_TOKENS = 1000;

    public void updateContext(String threadId) {
        try {
            ChatThread thread = chatThreadRepository.findById(threadId)
                    .orElseThrow(() -> new RuntimeException("Thread not found"));

            List<Message> messages = messageServiceImpl.findAllByThreadId(threadId);
            if (messages.isEmpty()) return;

            int lastIndex = Math.min(thread.getLastSummaryAtCount(), messages.size());

            List<Message> newMessages = messages.subList(lastIndex, messages.size());
            if (newMessages.isEmpty()) return;

            // Build clean conversation
            StringBuilder conversation = new StringBuilder();
            int tokens = 0;

            for (Message m : newMessages) {
                String role = m.getSender() == Sender.USER ? "User" : "Assistant";
                String text = clean(m.getText());

                String line = role + ": " + text + "\n";
                int t = TokenEstimator.estimateTokens(line);

                if (tokens + t > SUMMARY_TOKEN_LIMIT) break;

                conversation.append(line);
                tokens += t;
            }

            if (conversation.isEmpty()) return;

            // Generate fresh summary ONLY from raw messages
            String newSummary = geminiClient.generateContext(conversation.toString());

            String finalSummary;

            if (thread.getThreadContext() != null && !thread.getThreadContext().isEmpty()) {

                // Controlled merge (avoid drift)
                String mergeInput =
                        "Old summary:\n" + thread.getThreadContext() +
                                "\n\nNew facts:\n" + newSummary +
                                "\n\nMerge into a clean, concise memory. Remove redundancy.";

                finalSummary = geminiClient.generateContext(mergeInput);

            } else {
                finalSummary = newSummary;
            }

            // Hard trim if needed
            int finalTokens = TokenEstimator.estimateTokens(finalSummary);
            if (finalTokens > MAX_CONTEXT_TOKENS) {
                finalSummary = finalSummary.substring(0, Math.min(500, finalSummary.length()));
            }

            thread.setThreadContext(finalSummary);
            thread.setLastSummaryAtCount(messages.size());
            thread.setSummaryTokenCount(TokenEstimator.estimateTokens(finalSummary));

            chatThreadRepository.save(thread);

            log.info("Context updated for threadId={}", threadId);

        } catch (Exception e) {
            log.error("Context update failed for threadId={}: {}", threadId, e.getMessage());
        }
    }

    private String clean(String text) {
        if (text == null) return "";
        return text
                .replaceAll("(?i)user:\\s*", "")
                .replaceAll("(?i)assistant:\\s*", "")
                .replaceAll("(?i)ai:\\s*", "")
                .trim();
    }
}