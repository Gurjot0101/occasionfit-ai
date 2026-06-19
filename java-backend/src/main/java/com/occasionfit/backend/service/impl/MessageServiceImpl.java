package com.occasionfit.backend.service.impl;

import com.occasionfit.backend.model.Message;
import com.occasionfit.backend.model.enums.Sender;
import com.occasionfit.backend.repository.ChatThreadRepository;
import com.occasionfit.backend.repository.MessageRepository;
import com.occasionfit.backend.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ChatThreadRepository chatThreadRepository;

    public Message saveMessage(String threadId, String text, Sender sender, List<String> images, int tokens) {
        Message message = Message.builder().threadId(threadId).text(text).sender(sender).images(images).tokens(tokens).build();
        log.info("Saving message with threadId: {} text: {} images: {}", threadId, text, images.size());
        chatThreadRepository.incrementCounters(
                threadId,
                text.substring(0, Math.min(text.length(), 50)),
                1,
                tokens
        );
        return messageRepository.save(message);
    }

    public List<Message> findAllByThreadId(String threadId) {
        log.info("Finding all messages by threadId: {}", threadId);
        return messageRepository.findByThreadIdOrderByTimestampAsc(threadId);
    }

    public void deleteMessages(String threadId) {
        messageRepository.deleteByThreadId(threadId);
    }

    public List<Message> getRecentMessages(String threadId, int summaryTokens) {
        List<Message> messages =
                messageRepository.findTop10ByThreadIdOrderByTimestampDesc(threadId);

        List<Message> selected = new ArrayList<>();
        int total = summaryTokens;
        for (Message msg : messages) {
            int t = msg.getTokens() != null ? msg.getTokens() : 0;
            if (total + t > 3000) break;
            selected.add(msg);
            total += t;
        }
        Collections.reverse(selected);
        return selected;
    }
}
