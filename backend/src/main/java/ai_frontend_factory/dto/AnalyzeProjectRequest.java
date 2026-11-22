package ai_frontend_factory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnalyzeProjectRequest(
        @NotBlank(message = "Project description is required")
        @Size(max = 10000, message = "Project description must not exceed 10000 characters")
        String description
) {
}
