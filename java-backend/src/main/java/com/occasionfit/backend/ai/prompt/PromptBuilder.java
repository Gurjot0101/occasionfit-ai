package com.occasionfit.backend.ai.prompt;

import com.occasionfit.backend.agent.AgentAction;
import com.occasionfit.backend.agent.AgentContext;

public class PromptBuilder {

    private static final int MAX_SYNTHESIS_RESULT_LENGTH = 500;

    public static String buildPlannerPrompt(AgentContext ctx) {
        return """
                You are a planner for a fashion assistant.
                Return the exact tool sequence for the user's request.
                
                Available tools:
                - DIRECT_REPLY           : greetings, small talk, off-topic. Always alone.
                - ANALYZE_OUTFIT_IMAGE   : analyze a single uploaded image
                - COMPARE_OUTFIT_IMAGES  : compare 2+ uploaded outfit images
                - GENERATE_OUTFIT_IMAGE  : generate outfit image (text-to-image via Imagen)
                - GENERATE_TEXT_RESPONSE : send final text response to user
                
                Rules:
                - Always end with GENERATE_TEXT_RESPONSE unless using DIRECT_REPLY
                - If 2+ images + compare intent or no intent → [COMPARE_OUTFIT_IMAGES, GENERATE_TEXT_RESPONSE]
                - If 2+ images + generate intent             → [COMPARE_OUTFIT_IMAGES, GENERATE_OUTFIT_IMAGE, GENERATE_TEXT_RESPONSE]
                - If 1 image                                 → [ANALYZE_OUTFIT_IMAGE, GENERATE_TEXT_RESPONSE]
                - If 0 images + user refers to "my outfit/dress/look/style" → [DIRECT_REPLY] telling user to upload an image
                - If 0 images + generate intent              → [GENERATE_OUTFIT_IMAGE, GENERATE_TEXT_RESPONSE]
                - If 0 images + fashion question             → [GENERATE_TEXT_RESPONSE]
                - If greeting or off-topic                   → [DIRECT_REPLY]
                
                User message: %s
                Images uploaded: %d
                Thread context: %s
                
                Respond ONLY in this exact JSON format, no markdown:
                {"steps": ["TOOL_1", "TOOL_2"]}
                """.formatted(
                ctx.getUserMessage(),
                ctx.getBase64Images() != null ? ctx.getBase64Images().size() : 0,
                ctx.getThreadContext() != null ? ctx.getThreadContext() : "none"
        );
    }

    public static String buildSynthesisPrompt(AgentContext ctx) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(PromptTemplate.system());

        if (ctx.getThreadContext() != null && !ctx.getThreadContext().isEmpty())
            prompt.append("\nContext: ").append(ctx.getThreadContext());

        prompt.append("\n\nUser: ").append(ctx.getUserMessage());

        if (!ctx.getExecutedActions().isEmpty()) {
            prompt.append("\n\nInformation gathered:");
            for (AgentAction action : ctx.getExecutedActions()) {
                prompt.append("\n- ").append(truncate(action.getToolResult()));
            }
        }

        prompt.append("\n\nBased on the above, respond to the user:");
        return prompt.toString();
    }

    private static String truncate(String text) {
        if (text == null) return null;
        return text.length() > PromptBuilder.MAX_SYNTHESIS_RESULT_LENGTH ? text.substring(0, PromptBuilder.MAX_SYNTHESIS_RESULT_LENGTH) + "..." : text;
    }
}