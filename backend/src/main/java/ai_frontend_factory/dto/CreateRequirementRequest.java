package ai_frontend_factory.dto;

import ai_frontend_factory.entity.RequirementCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRequirementRequest(
        @NotBlank(message = "Requirement title is required")
        @Size(max = 200, message = "Requirement title must not exceed 200 characters")
        String title,

        @Size(max = 10000, message = "Requirement description must not exceed 10000 characters")
        String description,

        @NotNull(message = "Requirement category is required")
        RequirementCategory category,

        @NotBlank(message = "Requirement priority is required")
        @Size(max = 30, message = "Requirement priority must not exceed 30 characters")
        String priority
) {
}
