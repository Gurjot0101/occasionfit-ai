package com.occasionfit.backend.ai;

import com.occasionfit.backend.agent.AgentContext;
import com.occasionfit.backend.agent.AgentPlan;
import com.occasionfit.backend.agent.tools.AgentTool;
import com.occasionfit.backend.ai.client.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class PlannerService {

    private final GeminiClient geminiClient;

    public AgentPlan plan(AgentContext ctx) {
        try {
            return geminiClient.generatePlan(ctx);
        } catch (Exception e) {
            log.error("Plan parsing failed: {}", e.getMessage());
            return fallbackPlan();
        }
    }

    private AgentPlan fallbackPlan() {
        return AgentPlan.builder()
                .steps(List.of(AgentTool.GENERATE_TEXT_RESPONSE))
                .build();
    }
}