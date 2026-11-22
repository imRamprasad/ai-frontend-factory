package ai_frontend_factory.service;

import ai_frontend_factory.entity.Project;
import ai_frontend_factory.exception.ProjectNotFoundException;
import ai_frontend_factory.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    public Project createProject(String name, String description) {
        String normalizedName = validateName(name);

        Project project = new Project();
        project.setId(UUID.randomUUID().toString());
        project.setName(normalizedName);
        project.setDescription(description);
        project.setStatus("CREATED");
        return projectRepository.save(project);
    }

    public Project getProjectById(String id) {
        String normalizedId = validateId(id);
        return projectRepository.findById(normalizedId)
                .orElseThrow(() -> new ProjectNotFoundException(normalizedId));
    }

    public List<Project> getAllProjectsNewestFirst() {
        return projectRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Project> getProjectsByStatus(String status) {
        String normalizedStatus = validateStatus(status);
        return projectRepository.findByStatusOrderByCreatedAtDesc(normalizedStatus);
    }

    @Transactional
    public Project updateProject(String id, String name, String description) {
        Project project = getProjectById(id);
        project.setName(validateName(name));
        project.setDescription(description);
        return projectRepository.save(project);
    }

    @Transactional
    public void deleteProject(String id) {
        Project project = getProjectById(id);
        projectRepository.delete(project);
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Project name must not be blank");
        }
        return name.trim();
    }

    private String validateStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Project status must not be blank");
        }
        return status.trim();
    }

    private String validateId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Project ID must not be blank");
        }
        return id.trim();
    }
}
