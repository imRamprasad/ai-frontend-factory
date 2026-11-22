package ai_frontend_factory.service;

import ai_frontend_factory.entity.Project;
import ai_frontend_factory.entity.Requirement;
import ai_frontend_factory.entity.RequirementCategory;
import ai_frontend_factory.exception.ProjectNotFoundException;
import ai_frontend_factory.exception.RequirementNotFoundException;
import ai_frontend_factory.repository.ProjectRepository;
import ai_frontend_factory.repository.RequirementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequirementServiceTest {

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private RequirementService requirementService;

    @Test
    void createRequirementLinksToProject() {
        Project project = project("project-id");
        Requirement saved = requirement("requirement-id", project);

        when(projectRepository.findById("project-id")).thenReturn(Optional.of(project));
        when(requirementRepository.save(any(Requirement.class))).thenReturn(saved);

        Requirement result = requirementService.createRequirement(
                "project-id",
                "Authentication",
                "Use secure login",
                RequirementCategory.SECURITY,
                "HIGH"
        );

        assertEquals("requirement-id", result.getId());
        assertEquals(project, result.getProject());
        verify(requirementRepository).save(any(Requirement.class));
    }

    @Test
    void createRequirementThrowsWhenProjectMissing() {
        when(projectRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ProjectNotFoundException.class,
                () -> requirementService.createRequirement(
                        "missing",
                        "Authentication",
                        "Description",
                        RequirementCategory.SECURITY,
                        "HIGH"
                ));
    }

    @Test
    void getRequirementsUsesProjectScopedNewestFirstQuery() {
        Project project = project("project-id");
        when(projectRepository.findById("project-id")).thenReturn(Optional.of(project));
        when(requirementRepository.findByProjectIdOrderByCreatedAtDesc("project-id"))
                .thenReturn(List.of(requirement("one", project), requirement("two", project)));

        List<Requirement> requirements = requirementService.getRequirements("project-id");

        assertEquals(2, requirements.size());
        verify(requirementRepository).findByProjectIdOrderByCreatedAtDesc("project-id");
    }

    @Test
    void getRequirementThrowsWhenMissingFromProject() {
        Project project = project("project-id");
        when(projectRepository.findById("project-id")).thenReturn(Optional.of(project));
        when(requirementRepository.findByIdAndProjectId("missing", "project-id"))
                .thenReturn(Optional.empty());

        assertThrows(RequirementNotFoundException.class,
                () -> requirementService.getRequirement("project-id", "missing"));
    }

    @Test
    void deleteRequirementDeletesScopedRequirement() {
        Project project = project("project-id");
        Requirement requirement = requirement("requirement-id", project);
        when(projectRepository.findById("project-id")).thenReturn(Optional.of(project));
        when(requirementRepository.findByIdAndProjectId("requirement-id", "project-id"))
                .thenReturn(Optional.of(requirement));

        requirementService.deleteRequirement("project-id", "requirement-id");

        verify(requirementRepository).delete(requirement);
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
        requirement.setTitle("Authentication");
        requirement.setDescription("Use secure login");
        requirement.setCategory(RequirementCategory.SECURITY);
        requirement.setPriority("HIGH");
        return requirement;
    }
}
