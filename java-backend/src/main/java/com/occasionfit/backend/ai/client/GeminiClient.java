package com.occasionfit.backend.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.occasionfit.backend.agent.AgentContext;
import com.occasionfit.backend.agent.AgentPlan;
import com.occasionfit.backend.agent.tools.AgentTool;
import com.occasionfit.backend.ai.prompt.PromptBuilder;
import com.occasionfit.backend.ai.prompt.PromptCleaner;
import com.occasionfit.backend.ai.prompt.PromptTemplate;
import com.occasionfit.backend.model.Message;
import com.occasionfit.backend.model.enums.Sender;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;


@Service
@Log4j2
public class GeminiClient {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model.text}")
    private String modelText;

    @Value("${gemini.model.image}")
    private String modelImage;

    @Value("${gemini.model.vision}")
    private String modelVision;

    private Client client;

    @PostConstruct
    public void init() {
        client = new Client.Builder()
                .apiKey(apiKey)
                .build();
    }

    @Autowired
    private ObjectMapper objectMapper;

    // 1. Chat response
    public String generateResponse(String threadContext, String userMessage, List<Message> recentMessages) {
        try {
            StringBuilder prompt = new StringBuilder();

            prompt.append(PromptTemplate.system());

            prompt.append(
                    """
                            Keep response under 80 words.
                            Output must be plain text only.
                            Ask only 1 question.
                            If user is unclear, simplify.
                            
                            """
            );

            if (threadContext != null && !threadContext.isEmpty())
                prompt.append("Summary:\n").append(threadContext).append("\n\n");

            prompt.append("Conversation:\n");
            for (Message m : recentMessages) {
                String role = m.getSender() == Sender.USER ? "User" : "Assistant";
                prompt.append(role)
                        .append(": ")
                        .append(PromptCleaner.clean(m.getText()))
                        .append("\n");
            }

            prompt.append("User: ").append(userMessage);

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .temperature(0.4f)
                    .maxOutputTokens(2048)
                    .topP(0.95f)
                    .build();

            GenerateContentResponse response = client.models.generateContent(
                    modelText,
                    Content.fromParts(
                            Part.fromText(prompt.toString())
                    ),
                    config
            );

            String output = response.text();
            if (output == null || output.trim().isEmpty()) return "Could you clarify your requirement?";

            return PromptCleaner.clean(output);
        } catch (Exception e) {
            log.error("Gemini chat error: {}", e.getMessage());
            return "Sorry, I couldn't generate a response. Please try again.";
        }
    }

    // 2. Context generation from conversation
    public String generateContext(String text) {
        try {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .temperature(0.1f)
                    .maxOutputTokens(512)
                    .build();

            GenerateContentResponse response = client.models.generateContent(
                    modelText,
                    Content.fromParts(
                            Part.fromText(
                                    "Summarize this user intent in max 20 words.\n" +
                                            "Focus only on: occasion + clothing intent.\n\n" +
                                            "Input: " + text
                            )
                    ),
                    config
            );
            return response.text() != null ? response.text() : "";
        } catch (Exception e) {
            log.error("Gemini context error: {}", e.getMessage());
            return text.substring(0, Math.min(20, text.length()));
        }
    }

    // 3. Image understanding — analyze user's uploaded outfit image
    public String analyzeOutfitImage(String threadContext, String userMessage, List<Message> recentMessages, String base64Image) {
        try {
            StringBuilder prompt = new StringBuilder();

            prompt.append(PromptTemplate.system());
            prompt.append(
                    """
                            Analyze this outfit image and provide styling recommendations.
                            Keep explanation under 50 words.
                            Output must be plain text only.
                            
                            """
            );

            if (threadContext != null && !threadContext.isEmpty())
                prompt.append("Summary:\n").append(threadContext).append("\n\n");

            prompt.append("Conversation:\n");
            for (Message m : recentMessages) prompt.append(m.getSender()).append(": ").append(m.getText()).append("\n");
            prompt.append("User: ").append(userMessage);

            ImageData img = decodeImage(base64Image);

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .temperature(0.3f)
                    .maxOutputTokens(2048)
                    .build();

            GenerateContentResponse response = client.models.generateContent(
                    modelVision,
                    Content.fromParts(
                            Part.fromText(prompt.toString()),
                            Part.fromBytes(img.bytes, img.mimeType)  // decoded bytes + mime type
                    ),
                    config
            );
            return response.text() != null ? response.text() : "No response generated.";
        } catch (Exception e) {
            log.error("Gemini image error: {}", e.getMessage());
            return "Sorry, I couldn't analyze the image. Please try again.";
        }
    }

    // 4. Generate outfit image using Imagen
    public String generateOutfitImage(String prompt) throws Exception {
        try {

            GenerateContentConfig config =
                    GenerateContentConfig.builder()
                            .responseModalities(List.of("IMAGE"))
                            .build();

            GenerateContentResponse response =
                    client.models.generateContent(
                            modelImage,
                            "Generate a fashion outfit image: " +
                                    prompt +
                                    ". Professional fashion photography.",
                            config
                    );

            for (Part part : Objects.requireNonNull(response.parts())) {

                if (part.inlineData().isPresent()) {

                    byte[] imageBytes =
                            part.inlineData()
                                    .get()
                                    .data()
                                    .orElse(null);

                    if (imageBytes != null) {
                        return Base64.getEncoder()
                                .encodeToString(imageBytes);
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Gemini image gen error: {}", e.getMessage());
            throw new Exception(e.getMessage());
        }
    }

    // 5. Compare images
    public String analyzeImages(List<String> base64Images, String promptText) {
        try {
            List<Part> parts = new ArrayList<>();

            for (String base64 : base64Images) {
                ImageData img = decodeImage(base64);
                parts.add(Part.fromBytes(img.bytes(), img.mimeType));
            }

            parts.add(Part.fromText(promptText));

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .temperature(0.3f)
                    .maxOutputTokens(2048)
                    .build();

            GenerateContentResponse response = client.models.generateContent(
                    modelVision,
                    Content.fromParts(parts.toArray(new Part[0])),
                    config
            );

            return response.text() != null ? PromptCleaner.clean(response.text()) : "Could not compare outfits.";
        } catch (Exception e) {
            log.error("Gemini compare error: {}", e.getMessage());
            return "Sorry, I couldn't compare the images. Please try again.";
        }
    }

    /**
     * generatePlan function responds with a list of tools
     */
    public AgentPlan generatePlan(AgentContext ctx) throws Exception {
        String prompt = PromptBuilder.buildPlannerPrompt(ctx);
        GenerateContentConfig config = GenerateContentConfig.builder()
                .temperature(0.1f)
                .maxOutputTokens(512)
                .build();
        GenerateContentResponse response = client.models.generateContent(
                modelText,
                Content.fromParts(Part.fromText(prompt)),
                config
        );
        String raw = response.text();
        if (raw == null || raw.isBlank()) throw new Exception("Empty planner response");

        raw = raw.replaceAll("```json|```", "").trim();
        JsonNode root = objectMapper.readTree(raw);

        List<AgentTool> steps = new ArrayList<>();
        for (JsonNode step : root.get("steps"))
            steps.add(AgentTool.valueOf(step.asText()));

        return AgentPlan.builder().steps(steps).build();
    }

    /**
     * Final Synthesis of whole agentic workflow
     */
    public String synthesizeFinalResponse(AgentContext ctx) {
        try {
            String prompt = PromptBuilder.buildSynthesisPrompt(ctx);
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .temperature(0.2f)
                    .maxOutputTokens(1024)
                    .build();

            GenerateContentResponse response = client.models.generateContent(
                    modelText,
                    Content.fromParts(Part.fromText(prompt)),
                    config
            );

            String output = response.text();
            return output != null ? PromptCleaner.clean(output) : "Here are my recommendations!";
        } catch (Exception e) {
            log.error("Synthesis error: {}", e.getMessage());
            return "Sorry, I couldn't generate a response. Please try again.";
        }
    }
    
    private record ImageData(byte[] bytes, String mimeType) {
    }

    //imag decoder
    private ImageData decodeImage(String base64) {
        String mimeType = "image/jpeg";
        String data = base64;
        if (base64.contains(",")) {
            String[] split = base64.split(",");
            String header = split[0];
            data = split[1];
            if (header.contains("png")) mimeType = "image/png";
            else if (header.contains("webp")) mimeType = "image/webp";
        }
        return new ImageData(Base64.getDecoder().decode(data), mimeType);
    }
}