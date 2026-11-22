package ai_frontend_factory.service;

import ai_frontend_factory.ai.AiProvider;
import ai_frontend_factory.ai.AiRequest;
import ai_frontend_factory.ai.AiResponse;
import ai_frontend_factory.ai.GeneratedRequirement;
import ai_frontend_factory.ai.RequirementPriority;
import ai_frontend_factory.entity.Project;
import ai_frontend_factory.entity.Requirement;
import ai_frontend_factory.entity.RequirementCategory;
import ai_frontend_factory.exception.AiProviderNotConfiguredException;
import ai_frontend_factory.exception.ProjectNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiRequirementAnalyzerTest {

    @Test
    void fakeProviderOutputIsValidatedAndPersistedWithoutDuplicates() {
        AiProvider provider = mock(AiProvider.class);
        when(provider.generateRequirements(org.mockito.ArgumentMatchers.any(AiRequest.class)))
                .thenReturn(new AiResponse(
                        "E-commerce platform",
                        List.of(
                                generated("User login", RequirementCategory.SECURITY),
                                generated("User Login", RequirementCategory.FUNCTIONAL),
                                generated("Product management", RequirementCategory.FUNCTIONAL)
                        )
                ));

        ProjectService projectService = mock(ProjectService.class);
        Project project = project("project-id");
        when(projectService.getProjectById("project-id")).thenReturn(project);

        RequirementService requirementService = mock(RequirementService.class);
        when(requirementService.createRequirement(
                "project-id",
                "User login",
                "Description",
                RequirementCategory.SECURITY,
                "HIGH"
        )).thenAnswer(invocation -> requirement("requirement-1", project));
        when(requirementService.createRequirement(
                "project-id",
                "Product management",
                "Description",
                RequirementCategory.FUNCTIONAL,
                "HIGH"
        )).thenAnswer(invocation -> requirement("requirement-2", project));

        AiRequirementAnalyzer analyzer = new AiRequirementAnalyzer(
                Optional.of(provider),
                projectService,
                requirementService
        );

        AiResponse analyzed = analyzer.analyze(
                "project-id",
                "Build an e-commerce website"
        );
        var persisted = analyzer.persistAnalysis("project-id", analyzed);

        assertEquals(2, persisted.size());
        verify(requirementService).createRequirement(
                "project-id",
                "User login",
                "Description",
                RequirementCategory.SECURITY,
                "HIGH"
        );
        verify(requirementService).createRequirement(
                "project-id",
                "Product management",
                "Description",
                RequirementCategory.FUNCTIONAL,
                "HIGH"
        );
    }

    @Test
    void missingProviderReturnsClearConfigurationError() {
        ProjectService projectService = mock(ProjectService.class);
        when(projectService.getProjectById("project-id")).thenReturn(project("project-id"));
        AiRequirementAnalyzer analyzer = new AiRequirementAnalyzer(
                Optional.empty(),
                projectService,
                mock(RequirementService.class)
        );

        assertThrows(
                AiProviderNotConfiguredException.class,
                () -> analyzer.analyze("project-id", "Build a website")
        );
    }

    @Test
    void missingProjectIsPreserved() {
        ProjectService projectService = mock(ProjectService.class);
        when(projectService.getProjectById("missing"))
                .thenThrow(new ProjectNotFoundException("missing"));
        AiRequirementAnalyzer analyzer = new AiRequirementAnalyzer(
                Optional.empty(),
                projectService,
                mock(RequirementService.class)
        );

        assertThrows(
                ProjectNotFoundException.class,
                () -> analyzer.analyze("missing", "Build a website")
        );
    }

    @Test
    void invalidAiRequirementIsRejected() {
        AiProvider provider = mock(AiProvider.class);
        when(provider.generateRequirements(org.mockito.ArgumentMatchers.any(AiRequest.class)))
                .thenReturn(new AiResponse(
                        "Summary",
                        List.of(new GeneratedRequirement(
                                " ",
                                "Description",
                                RequirementCategory.FUNCTIONAL,
                                RequirementPriority.HIGH
                        ))
                ));
        AiRequirementAnalyzer analyzer = new AiRequirementAnalyzer(
                Optional.of(provider),
                projectServiceWithProject(),
                mock(RequirementService.class)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> analyzer.analyze("project-id", "Build a website")
        );
    }

    private ProjectService projectServiceWithProject() {
        ProjectService projectService = mock(ProjectService.class);
        when(projectService.getProjectById("project-id")).thenReturn(project("project-id"));
        return projectService;
    }

    private Project project(String id) {
        Project project = new Project();
        project.setId(id);
        project.setName("Factory");
        project.setStatus("CREATED");
        return project;
    }

    private Requirement requirement(String id, Project project) {
        Requirement requirement = new Requirement();
        requirement.setId(id);
        requirement.setProject(project);
        requirement.setTitle("Requirement");
        requirement.setDescription("Description");
        requirement.setCategory(RequirementCategory.FUNCTIONAL);
        requirement.setPriority("HIGH");
        return requirement;
    }

    private GeneratedRequirement generated(
            String title,
            RequirementCategory category
    ) {
        return new GeneratedRequirement(
                title,
                "Description",
                category,
                RequirementPriority.HIGH
        );
    }
}
