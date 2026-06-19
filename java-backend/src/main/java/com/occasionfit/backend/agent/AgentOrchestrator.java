package com.occasionfit.backend.agent;

import com.occasionfit.backend.agent.tools.AgentTool;
import com.occasionfit.backend.agent.tools.ToolExecutionResult;
import com.occasionfit.backend.agent.tools.ToolExecutor;
import com.occasionfit.backend.ai.PlannerService;
import com.occasionfit.backend.ai.client.GeminiClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Log4j2
public class AgentOrchestrator {

    private final PlannerService plannerService;
    private final GeminiClient geminiClient;
    private final Map<AgentTool, ToolExecutor> toolRegistry;

    @Autowired
    public AgentOrchestrator(PlannerService plannerService,
                             GeminiClient geminiClient,
                             List<ToolExecutor> tools) {
        this.plannerService = plannerService;
        this.geminiClient = geminiClient;
        this.toolRegistry = tools.stream()
                .collect(Collectors.toMap(ToolExecutor::getToolType, t -> t));
    }

    public AgentResult run(AgentContext ctx) {
        AgentPlan plan = plannerService.plan(ctx);
        log.info("Execution plan: {}", plan.getSteps());

        String generatedImage = null;

        for (AgentTool tool : plan.getSteps()) {
            log.info("Executing tool: {}", tool);
            ToolExecutionResult result = executeTool(tool, ctx);

            ctx.addAction(AgentAction.builder()
                    .tool(tool)
                    .toolResult(result.isSuccess() ? result.getOutput() : result.getErrorMessage())
                    .build());

            if (tool == AgentTool.GENERATE_OUTFIT_IMAGE && result.isSuccess())
                generatedImage = result.getOutput();

            if (!result.isSuccess()) {
                log.warn("Tool {} failed — aborting plan", tool);
                break;
            }
        }

        return buildResult(ctx, generatedImage);
    }

    private AgentResult buildResult(AgentContext ctx, String generatedImage) {
        AgentAction last = ctx.getExecutedActions().isEmpty()
                ? null
                : ctx.getExecutedActions().getLast();

        boolean lastProducedText = last != null && (
                last.getTool() == AgentTool.GENERATE_TEXT_RESPONSE ||
                        last.getTool() == AgentTool.DIRECT_REPLY
        );

        String text = lastProducedText
                ? last.getToolResult()
                : geminiClient.synthesizeFinalResponse(ctx);

        return new AgentResult(text, generatedImage);
    }

    private ToolExecutionResult executeTool(AgentTool tool, AgentContext ctx) {
        ToolExecutor executor = toolRegistry.get(tool);
        if (executor == null) {
            log.error("No executor registered for tool: {}", tool);
            return ToolExecutionResult.failure("Unsupported tool: " + tool);
        }
        return executor.execute(ctx);
    }
}