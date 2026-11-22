package ai_frontend_factory.dto;

import ai_frontend_factory.entity.RequirementCategory;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record AiAnalysisResponse(
        String projectId,
        String summary,
        List<RequirementResponse> generatedRequirements,
        Map<RequirementCategory, Integer> requirementCountsByCategory,
        int totalRequirementsGenerated
) {
    public static AiAnalysisResponse from(
            String projectId,
            String summary,
            List<RequirementResponse> requirements
    ) {
        Map<RequirementCategory, Integer> counts =
                new EnumMap<>(RequirementCategory.class);
        for (RequirementCategory category : RequirementCategory.values()) {
            counts.put(category, 0);
        }
        for (RequirementResponse requirement : requirements) {
            counts.compute(
                    requirement.category(),
                    (category, count) -> count + 1
            );
        }
        return new AiAnalysisResponse(
                projectId,
                summary,
                requirements,
                counts,
                requirements.size()
        );
    }
}
