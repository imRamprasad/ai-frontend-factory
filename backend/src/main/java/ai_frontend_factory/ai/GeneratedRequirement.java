package ai_frontend_factory.ai;

import ai_frontend_factory.entity.RequirementCategory;

public record GeneratedRequirement(
        String title,
        String description,
        RequirementCategory category,
        RequirementPriority priority
) {
}
