package com.occasionfit.backend.repository.impl;

import com.occasionfit.backend.model.ChatThread;
import com.occasionfit.backend.repository.ChatThreadRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChatThreadRepositoryImpl implements ChatThreadRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public void incrementCounters(String threadId, String lastMessageText, int messageDelta, int tokenDelta) {

        Query query = Query.query(Criteria.where("_id").is(threadId));

        Update update = new Update()
                .set("lastMessageText", lastMessageText)
                .inc("messageCount", messageDelta)
                .inc("totalTokens", tokenDelta);

        mongoTemplate.updateFirst(query, update, ChatThread.class);
    }
}