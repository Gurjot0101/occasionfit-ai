package com.occasionfit.backend.agent;

import com.occasionfit.backend.agent.tools.AgentTool;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AgentPlan {
    private List<AgentTool> steps;
}