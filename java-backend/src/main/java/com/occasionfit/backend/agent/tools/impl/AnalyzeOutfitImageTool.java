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
public class AnalyzeOutfitImageTool implements ToolExecutor {

    private final GeminiClient geminiVisionService;

    @Override
    public AgentTool getToolType() {
        return AgentTool.ANALYZE_OUTFIT_IMAGE;
    }

    @Override
    public ToolExecutionResult execute(AgentContext ctx) {
        try {
            String result = geminiVisionService.analyzeOutfitImage(
                    ctx.getThreadContext(),
                    ctx.getUserMessage(),
                    ctx.getRecentMessages(),
                    ctx.getBase64Images().getFirst()
            );
            return ToolExecutionResult.success(result);
        } catch (Exception e) {
            log.warn("Error: {}", e.getMessage());
            return ToolExecutionResult.failure("Unable to do image analysis, maybe image is not attached. Please try again later.");
        }
    }
}