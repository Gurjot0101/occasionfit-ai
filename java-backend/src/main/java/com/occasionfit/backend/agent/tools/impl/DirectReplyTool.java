package com.occasionfit.backend.agent.tools.impl;

import com.occasionfit.backend.agent.AgentContext;
import com.occasionfit.backend.agent.tools.AgentTool;
import com.occasionfit.backend.agent.tools.ToolExecutionResult;
import com.occasionfit.backend.agent.tools.ToolExecutor;
import com.occasionfit.backend.ai.client.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Component
@Log4j2
@RequiredArgsConstructor
public class DirectReplyTool implements ToolExecutor {

    private final GeminiClient geminiChatService;

    @Override
    public AgentTool getToolType() {
        return AgentTool.DIRECT_REPLY;
    }

    @Override
    public ToolExecutionResult execute(AgentContext ctx) {
        String response = geminiChatService.generateResponse(
                ctx.getThreadContext(),
                ctx.getUserMessage(),
                ctx.getRecentMessages()
        );
        return ToolExecutionResult.success(response);
    }
}
