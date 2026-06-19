package com.occasionfit.backend.agent;

import com.occasionfit.backend.model.Message;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class AgentContext {
    private String userMessage;
    private String threadContext;
    private List<String> base64Images;
    private List<Message> recentMessages;

    @Builder.Default
    private List<AgentAction> executedActions = new ArrayList<>();

    public void addAction(AgentAction action) {
        executedActions.add(action);
    }

    public boolean hasExecutedActions() {
        return executedActions != null && !executedActions.isEmpty();
    }

    public String getLastToolResult() {
        if (!hasExecutedActions()) return null;
        return executedActions.getLast().getToolResult();
    }
}