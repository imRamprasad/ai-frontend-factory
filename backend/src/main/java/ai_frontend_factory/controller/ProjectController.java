package ai_frontend_factory.controller;

import ai_frontend_factory.dto.CreateProjectRequest;
import ai_frontend_factory.dto.ProjectResponse;
import ai_frontend_factory.dto.UpdateProjectRequest;
import ai_frontend_factory.entity.Project;
import ai_frontend_factory.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request
    ) {
        Project project = projectService.createProject(request.name(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectResponse.from(project));
    }

    @GetMapping
    public List<ProjectResponse> getProjects() {
        return projectService.getAllProjectsNewestFirst().stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ProjectResponse getProject(@PathVariable String id) {
        return ProjectResponse.from(projectService.getProjectById(id));
    }

    @PutMapping("/{id}")
    public ProjectResponse updateProject(
            @PathVariable String id,
            @Valid @RequestBody UpdateProjectRequest request
    ) {
        Project project = projectService.updateProject(
                id,
                request.name(),
                request.description()
        );
        return ProjectResponse.from(project);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable String id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
