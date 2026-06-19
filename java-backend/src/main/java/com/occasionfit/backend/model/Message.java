package com.occasionfit.backend.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.occasionfit.backend.model.enums.Sender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {
    @Id
    private String id;
    @Indexed
    private String threadId;      // reference to Thread
    private Sender sender;       // "user" or "ai"
    private String text;
    private List<String> images;
    private Integer tokens;
    @CreatedDate
    private LocalDateTime timestamp;
}