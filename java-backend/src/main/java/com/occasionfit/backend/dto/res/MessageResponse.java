package com.occasionfit.backend.dto.res;

import com.occasionfit.backend.model.ChatThread;
import com.occasionfit.backend.model.Message;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageResponse {
    Message message;
    ChatThread thread;
}
