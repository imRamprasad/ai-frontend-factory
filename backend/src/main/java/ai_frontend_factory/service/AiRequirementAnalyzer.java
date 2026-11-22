package ai_frontend_factory.service;

import ai_frontend_factory.ai.AiProvider;
import ai_frontend_factory.ai.AiRequest;
import ai_frontend_factory.ai.AiResponse;
import ai_frontend_factory.ai.GeneratedRequirement;
import ai_frontend_factory.dto.RequirementResponse;
import ai_frontend_factory.entity.Requirement;
import ai_frontend_factory.exception.AiProviderNotConfiguredException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Locale;

@Service
public class AiRequirementAnalyzer {

    private final Optional<AiProvider> aiProvider;
    private final ProjectService projectService;
    private final RequirementService requirementService;

    public AiRequirementAnalyzer(
            Optional<AiProvider> aiProvider,
            ProjectService projectService,
            RequirementService requirementService
    ) {
        this.aiProvider = aiProvider;
        this.projectService = projectService;
        this.requirementService = requirementService;
    }

    public AiResponse analyze(String projectId, String description) {
        projectService.getProjectById(projectId);

        if (aiProvider.isEmpty()) {
            throw new AiProviderNotConfiguredException();
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Project description must not be blank");
        }

        AiResponse response = aiProvider.get().generateRequirements(
                new AiRequest(description.trim())
        );
        return validateAndDeduplicate(response);
    }

    public List<RequirementResponse> analyzeAndPersist(
            String projectId,
            String description
    ) {
        AiResponse response = analyze(projectId, description);
        return persistAnalysis(projectId, response);
    }

    public List<RequirementResponse> persistAnalysis(
            String projectId,
            AiResponse response
    ) {
        Set<String> uniqueWithinAnalysis = new LinkedHashSet<>();
        List<RequirementResponse> persisted = new ArrayList<>();

        for (GeneratedRequirement generated : response.requirements()) {
            String duplicateKey = generated.title().trim().toLowerCase();
            if (!uniqueWithinAnalysis.add(duplicateKey)) {
                continue;
            }

            Requirement requirement = requirementService.createRequirement(
                    projectId,
                    generated.title(),
                    generated.description(),
                    generated.category(),
                    generated.priority().name()
            );
            persisted.add(RequirementResponse.from(requirement));
        }
        return persisted;
    }

    private AiResponse validateAndDeduplicate(AiResponse response) {
        if (response == null) {
            throw new IllegalArgumentException("AI provider returned no analysis response");
        }
        if (response.summary() == null || response.summary().isBlank()) {
            throw new IllegalArgumentException("AI provider returned no analysis summary");
        }
        if (response.requirements() == null) {
            throw new IllegalArgumentException("AI provider returned no requirements");
        }

        Set<String> titles = new HashSet<>();
        List<GeneratedRequirement> validated = new ArrayList<>();
        for (GeneratedRequirement requirement : response.requirements()) {
            if (requirement == null
                    || requirement.title() == null
                    || requirement.title().isBlank()
                    || requirement.description() == null
                    || requirement.category() == null
                    || requirement.priority() == null) {
                throw new IllegalArgumentException("AI provider returned an invalid requirement");
            }
            if (requirement.title().trim().length() > 200) {
                throw new IllegalArgumentException("AI requirement title exceeds 200 characters");
            }
            if (requirement.description().length() > 10000) {
                throw new IllegalArgumentException(
                        "AI requirement description exceeds 10000 characters"
                );
            }

            String key = requirement.title().trim().toLowerCase(Locale.ROOT);
            if (titles.add(key)) {
                validated.add(requirement);
            }
        }

        if (validated.isEmpty()) {
            throw new IllegalArgumentException("AI provider returned no valid requirements");
        }
        return new AiResponse(response.summary().trim(), validated);
    }
}
