package com.occasionfit.backend.scheduler;

import com.occasionfit.backend.model.ChatThread;
import com.occasionfit.backend.repository.ChatThreadRepository;
import com.occasionfit.backend.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Log4j2
public class CleanupScheduler {

    private final ChatThreadRepository threadRepository;
    private final MessageRepository messageRepository;

    @Scheduled(cron = "0 0 0 * * *")  // runs every day at midnight
    public void deleteOldThreadsAndMessages() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);

        // find old threads
        List<ChatThread> oldThreads = threadRepository.findByCreatedAtBefore(cutoff);

        oldThreads.forEach(thread -> {
            messageRepository.deleteByThreadId(thread.getId());
            threadRepository.delete(thread);
            log.info("Deleted old thread: {}", thread.getId());
        });
    }
}