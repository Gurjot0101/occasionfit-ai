package com.occasionfit.backend.dto.req;

import lombok.Data;

import java.util.List;

@Data
public class MessageRequest {
    private String message;
    private String threadId;
    private List<String> images;
}
