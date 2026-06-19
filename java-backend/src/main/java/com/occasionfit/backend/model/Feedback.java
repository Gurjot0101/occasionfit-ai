package com.occasionfit.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "feedbacks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {

    @Id
    private String id;

    private String userId;      // reference to User
    private String userName;    // denormalized
    private String userEmail;   // denormalized

    private String feedback;
    private int rating;

    @CreatedDate
    private LocalDateTime createdAt;
}