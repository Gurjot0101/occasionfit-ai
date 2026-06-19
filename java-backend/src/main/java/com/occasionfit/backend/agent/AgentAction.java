package com.occasionfit.backend.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.occasionfit.backend.agent.tools.AgentTool;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentAction {
    private AgentTool tool;
    private String reason;
    private String toolResult;
}