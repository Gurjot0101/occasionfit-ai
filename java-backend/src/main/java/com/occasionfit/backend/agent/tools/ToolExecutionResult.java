package com.occasionfit.backend.agent.tools;

import lombok.Getter;

@Getter
public class ToolExecutionResult {

    private final String output;
    private final boolean success;
    private final String errorMessage;

    private ToolExecutionResult(String output, boolean success, String errorMessage) {
        this.output = output;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public static ToolExecutionResult success(String output) {
        return new ToolExecutionResult(output, true, null);
    }

    public static ToolExecutionResult failure(String errorMessage) {
        return new ToolExecutionResult(null, false, errorMessage);
    }
}