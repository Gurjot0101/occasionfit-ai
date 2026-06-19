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
@RequiredArgsConstructor
@Log4j2
public class GenerateOutfitImageTool implements ToolExecutor {

    private final GeminiClient geminiImageGenService;

    @Override
    public AgentTool getToolType() {
        return AgentTool.GENERATE_OUTFIT_IMAGE;
    }

    @Override
    public ToolExecutionResult execute(AgentContext ctx) {
        try {
            String imageUrl = geminiImageGenService.generateOutfitImage(ctx.getUserMessage());
            return ToolExecutionResult.success(imageUrl);
        } catch (Exception e) {
            log.warn("Image generation quota exceeded or unavailable: {}", e.getMessage());
            return ToolExecutionResult.failure("You exceeded your current quota for image generation, please check your plan and billing details.");
        }
    }
}