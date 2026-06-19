package com.occasionfit.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "threads")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatThread {
    @Id
    private String id;
    @Indexed
    private String userId;        // reference to User
    @CreatedDate
    private LocalDateTime createdAt;
    private String threadContext;
    private Integer messageCount;
    private Integer totalTokens;
    private Integer lastSummaryAtCount;
    private Integer summaryTokenCount;
    private String lastMessageText;
    @LastModifiedDate
    private LocalDateTime updatedAt;
}