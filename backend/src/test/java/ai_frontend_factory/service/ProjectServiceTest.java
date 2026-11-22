package ai_frontend_factory.service;

import ai_frontend_factory.entity.Project;
import ai_frontend_factory.exception.ProjectNotFoundException;
import ai_frontend_factory.repository.ProjectRepository;
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
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void createProjectSetsDefaultsAndSaves() {
        when(projectRepository.save(any(Project.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Project project = projectService.createProject("  Factory  ", "Description");

        assertEquals("Factory", project.getName());
        assertEquals("Description", project.getDescription());
        assertEquals("CREATED", project.getStatus());
        assertEquals(36, project.getId().length());
        verify(projectRepository).save(project);
    }

    @Test
    void getProjectByIdThrowsWhenMissing() {
        when(projectRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ProjectNotFoundException.class,
                () -> projectService.getProjectById("missing"));
    }

    @Test
    void getAllProjectsUsesNewestFirstRepositoryMethod() {
        Project first = new Project();
        Project second = new Project();
        when(projectRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(first, second));

        List<Project> projects = projectService.getAllProjectsNewestFirst();

        assertEquals(List.of(first, second), projects);
        verify(projectRepository).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void updateProjectChangesNameAndDescription() {
        Project project = new Project();
        project.setId("project-id");
        when(projectRepository.findById("project-id")).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        Project updated = projectService.updateProject(
                "project-id", "  Updated name  ", "Updated description");

        assertEquals("Updated name", updated.getName());
        assertEquals("Updated description", updated.getDescription());
        verify(projectRepository).save(project);
    }

    @Test
    void deleteProjectUsesFoundEntity() {
        Project project = new Project();
        project.setId("project-id");
        when(projectRepository.findById("project-id")).thenReturn(Optional.of(project));

        projectService.deleteProject("project-id");

        verify(projectRepository).delete(project);
    }
}
