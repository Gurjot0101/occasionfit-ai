package com.occasionfit.backend.agent.tools;

import com.occasionfit.backend.agent.AgentContext;

public interface ToolExecutor {
    AgentTool getToolType();
    ToolExecutionResult execute(AgentContext ctx);
}
