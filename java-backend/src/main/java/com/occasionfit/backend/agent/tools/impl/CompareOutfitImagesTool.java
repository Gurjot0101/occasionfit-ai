package com.occasionfit.backend.agent.tools.impl;

import com.occasionfit.backend.agent.AgentContext;
import com.occasionfit.backend.agent.tools.AgentTool;
import com.occasionfit.backend.agent.tools.ToolExecutionResult;
import com.occasionfit.backend.agent.tools.ToolExecutor;
import com.occasionfit.backend.ai.client.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Log4j2
@RequiredArgsConstructor
public class CompareOutfitImagesTool implements ToolExecutor {

    private final GeminiClient geminiVisionService;

    @Override
    public AgentTool getToolType() {
        return AgentTool.COMPARE_OUTFIT_IMAGES;
    }

    @Override
    public ToolExecutionResult execute(AgentContext ctx) {

        List<String> images = ctx.getBase64Images();

        if (images == null || images.size() < 2) {
            return ToolExecutionResult.failure("At least 2 images required for comparison.");
        }
        try {
            String prompt = """
                    Compare these %d outfits. For each:
                    - Describe the style, colors, and fit
                    - Assess occasion suitability
                    Then recommend which is better and why.
                    User context: %s
                    """.formatted(images.size(), ctx.getUserMessage());

            String result = geminiVisionService.analyzeImages(images, prompt);
            return ToolExecutionResult.success(result);
        } catch (Exception e) {
            log.warn("Error: {}", e.getMessage());
            return ToolExecutionResult.failure("Unable to do image analysis, maybe image is not attached. Please try again later.");
        }
    }
}