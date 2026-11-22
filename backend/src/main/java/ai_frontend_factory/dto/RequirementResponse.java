package ai_frontend_factory.dto;

import ai_frontend_factory.entity.Requirement;
import ai_frontend_factory.entity.RequirementCategory;

import java.time.LocalDateTime;

public record RequirementResponse(
        String id,
        String projectId,
        String title,
        String description,
        RequirementCategory category,
        String priority,
        LocalDateTime createdAt
) {
    public static RequirementResponse from(Requirement requirement) {
        return new RequirementResponse(
                requirement.getId(),
                requirement.getProject().getId(),
                requirement.getTitle(),
                requirement.getDescription(),
                requirement.getCategory(),
                requirement.getPriority(),
                requirement.getCreatedAt()
        );
    }
}
