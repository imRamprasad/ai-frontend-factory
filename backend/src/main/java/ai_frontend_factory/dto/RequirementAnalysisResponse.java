package ai_frontend_factory.dto;

import ai_frontend_factory.entity.RequirementCategory;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record RequirementAnalysisResponse(
        String projectId,
        String summary,
        List<RequirementResponse> requirements,
        Map<RequirementCategory, Integer> requirementCountsByCategory,
        int totalRequirements
) {
    public static RequirementAnalysisResponse from(
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
        return new RequirementAnalysisResponse(
                projectId,
                summary,
                requirements,
                counts,
                requirements.size()
        );
    }
}
