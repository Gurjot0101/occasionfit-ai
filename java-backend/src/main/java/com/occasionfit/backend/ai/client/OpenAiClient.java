package com.occasionfit.backend.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.occasionfit.backend.agent.AgentContext;
import com.occasionfit.backend.agent.AgentPlan;
import com.occasionfit.backend.agent.tools.AgentTool;
import com.occasionfit.backend.ai.prompt.PromptBuilder;
import com.occasionfit.backend.ai.prompt.PromptCleaner;
import com.occasionfit.backend.ai.prompt.PromptTemplate;
import com.occasionfit.backend.model.Message;
import com.occasionfit.backend.model.enums.Sender;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Log4j2
public class OpenAiClient {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model.text:gpt-4o}")
    private String modelText;

    @Value("${openai.model.vision:gpt-4o}")
    private String modelVision;

    @Value("${openai.model.image:dall-e-3}")
    private String modelImage;

    private static final String CHAT_URL = "https://api.openai.com/v1/chat/completions";
    private static final String IMAGE_URL = "https://api.openai.com/v1/images/generations";

    private final RestTemplate restTemplate = new RestTemplate();

    // ── helpers ───────────────────────────────────────────────────────────────

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    /**
     * Sends a chat completion request and returns the first choice text.
     */
    private String chat(List<Map<String, Object>> messages) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelText);
        body.put("messages", messages);
        body.put("max_tokens", 300);

        ResponseEntity<Map> response = restTemplate.exchange(
                CHAT_URL, HttpMethod.POST,
                new HttpEntity<>(body, headers()), Map.class
        );

        return extractText(response.getBody());
    }

    /**
     * Sends a vision chat request (text + images).
     */
    private String vision(List<Map<String, Object>> messages) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelVision);
        body.put("messages", messages);
        body.put("max_tokens", 500);

        ResponseEntity<Map> response = restTemplate.exchange(
                CHAT_URL, HttpMethod.POST,
                new HttpEntity<>(body, headers()), Map.class
        );

        return extractText(response.getBody());
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map body) {
        if (body == null) return null;
        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
        if (choices == null || choices.isEmpty()) return null;
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return message != null ? (String) message.get("content") : null;
    }

    /**
     * Builds a system + user message list for simple chat.
     */
    private List<Map<String, Object>> buildChatMessages(String systemPrompt, String userPrompt) {
        return List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        );
    }

    /**
     * Wraps a base64 image into an OpenAI image_url content part.
     */
    private Map<String, Object> imageContentPart(String base64Image) {
        String mimeType = "image/jpeg";
        String data = base64Image;

        if (base64Image.contains(",")) {
            String[] split = base64Image.split(",");
            String header = split[0];
            data = split[1];
            if (header.contains("png")) mimeType = "image/png";
            else if (header.contains("webp")) mimeType = "image/webp";
        }

        String dataUri = "data:" + mimeType + ";base64," + data;
        return Map.of(
                "type", "image_url",
                "image_url", Map.of("url", dataUri, "detail", "high")
        );
    }

    // ── public API (mirrors GeminiClient) ────────────────────────────────────

    // 1. Chat response
    public String generateResponse(String threadContext, String userMessage, List<Message> recentMessages) {
        try {
            StringBuilder system = new StringBuilder();
            system.append(PromptTemplate.system());
            system.append("""
                    Keep response under 50 words.
                    Output must be plain text only.
                    Ask only 1 question.
                    If user is unclear, simplify.
                    """);

            if (threadContext != null && !threadContext.isEmpty())
                system.append("Summary:\n").append(threadContext).append("\n\n");

            StringBuilder conversation = new StringBuilder("Conversation:\n");
            for (Message m : recentMessages) {
                String role = m.getSender() == Sender.USER ? "User" : "Assistant";
                conversation.append(role).append(": ")
                        .append(PromptCleaner.clean(m.getText())).append("\n");
            }
            conversation.append("User: ").append(userMessage);

            String output = chat(buildChatMessages(system.toString(), conversation.toString()));
            if (output == null || output.isBlank()) return "Could you clarify your requirement?";
            return PromptCleaner.clean(output);
        } catch (Exception e) {
            log.error("OpenAI chat error: {}", e.getMessage());
            return "Sorry, I couldn't generate a response. Please try again.";
        }
    }

    // 2. Context generation
    public String generateContext(String text) {
        try {
            String prompt = "Summarize this user intent in max 20 words.\n"
                    + "Focus only on: occasion + clothing intent.\n\nInput: " + text;
            String output = chat(buildChatMessages(PromptTemplate.system(), prompt));
            return output != null ? output : "";
        } catch (Exception e) {
            log.error("OpenAI context error: {}", e.getMessage());
            return text.substring(0, Math.min(20, text.length()));
        }
    }

    // 3. Analyze single outfit image
    public String analyzeOutfitImage(String threadContext, String userMessage,
                                     List<Message> recentMessages, String base64Image) {
        try {
            StringBuilder textPrompt = new StringBuilder();
            textPrompt.append(PromptTemplate.system());
            textPrompt.append("""
                    Analyze this outfit image and provide styling recommendations.
                    Keep explanation under 50 words.
                    Output must be plain text only.
                    """);

            if (threadContext != null && !threadContext.isEmpty())
                textPrompt.append("Summary:\n").append(threadContext).append("\n\n");

            textPrompt.append("Conversation:\n");
            for (Message m : recentMessages)
                textPrompt.append(m.getSender()).append(": ").append(m.getText()).append("\n");
            textPrompt.append("User: ").append(userMessage);

            // vision message: text + image
            List<Object> contentParts = List.of(
                    Map.of("type", "text", "text", textPrompt.toString()),
                    imageContentPart(base64Image)
            );

            List<Map<String, Object>> messages = List.of(
                    Map.of("role", "user", "content", contentParts)
            );

            String output = vision(messages);
            return output != null ? output : "No response generated.";
        } catch (Exception e) {
            log.error("OpenAI image analyze error: {}", e.getMessage());
            return "Sorry, I couldn't analyze the image. Please try again.";
        }
    }

    // 4. Generate outfit image via DALL-E
    public String generateOutfitImage(String prompt) throws Exception {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", modelImage);
            body.put("prompt", "Generate a fashion outfit image: " + prompt + ". Professional fashion photography.");
            body.put("n", 1);
            body.put("size", "1024x1024");
            body.put("response_format", "b64_json");

            ResponseEntity<Map> response = restTemplate.exchange(
                    IMAGE_URL, HttpMethod.POST,
                    new HttpEntity<>(body, headers()), Map.class
            );

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) Objects.requireNonNull(response.getBody()).get("data");
            if (data == null || data.isEmpty()) return null;

            String b64 = (String) data.get(0).get("b64_json");
            return b64; // already base64, no header prefix
        } catch (Exception e) {
            log.error("OpenAI image gen error: {}", e.getMessage());
            throw new Exception(e.getMessage());
        }
    }

    // 5. Compare / analyze multiple images
    public String analyzeImages(List<String> base64Images, String promptText) {
        try {
            List<Object> contentParts = new ArrayList<>();
            contentParts.add(Map.of("type", "text", "text", promptText));

            for (String base64 : base64Images)
                contentParts.add(imageContentPart(base64));

            List<Map<String, Object>> messages = List.of(
                    Map.of("role", "user", "content", contentParts)
            );

            String output = vision(messages);
            return output != null ? PromptCleaner.clean(output) : "Could not compare outfits.";
        } catch (Exception e) {
            log.error("OpenAI compare error: {}", e.getMessage());
            return "Sorry, I couldn't compare the images. Please try again.";
        }
    }

    // 6. Generate execution plan
    public AgentPlan generatePlan(AgentContext ctx) throws Exception {
        String promptText = PromptBuilder.buildPlannerPrompt(ctx);
        String raw = chat(buildChatMessages(
                "You are a planner. Return only valid JSON. No markdown.",
                promptText
        ));

        if (raw == null || raw.isBlank()) throw new Exception("Empty planner response");

        raw = raw.replaceAll("```json|```", "").trim();
        JsonNode root = new ObjectMapper().readTree(raw);

        List<AgentTool> steps = new ArrayList<>();
        for (JsonNode step : root.get("steps"))
            steps.add(AgentTool.valueOf(step.asText()));

        log.info("OpenAI Execution plan: {}", steps);
        return AgentPlan.builder().steps(steps).build();
    }

    // 7. Synthesize final response from accumulated actions
    public String synthesizeFinalResponse(AgentContext ctx) {
        try {
            String promptText = PromptBuilder.buildSynthesisPrompt(ctx);
            String output = chat(buildChatMessages(PromptTemplate.system(), promptText));
            return output != null ? PromptCleaner.clean(output) : "Here are my recommendations!";
        } catch (Exception e) {
            log.error("OpenAI synthesis error: {}", e.getMessage());
            return "Sorry, I couldn't generate a response. Please try again.";
        }
    }
}