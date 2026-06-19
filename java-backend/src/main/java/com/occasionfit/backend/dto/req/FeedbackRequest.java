package com.occasionfit.backend.dto.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FeedbackRequest {
    @NotBlank
    private String feedback;

    @Min(1)
    @Max(5)
    private int rating;
}
