package com.occasionfit.backend.repository;

public interface ChatThreadRepositoryCustom {
    void incrementCounters(String threadId, String lastMessageText, int messageDelta, int tokenDelta);
}