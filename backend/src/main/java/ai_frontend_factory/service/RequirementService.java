package ai_frontend_factory.service;

import ai_frontend_factory.entity.Project;
import ai_frontend_factory.entity.Requirement;
import ai_frontend_factory.entity.RequirementCategory;
import ai_frontend_factory.exception.ProjectNotFoundException;
import ai_frontend_factory.exception.RequirementNotFoundException;
import ai_frontend_factory.repository.ProjectRepository;
import ai_frontend_factory.repository.RequirementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RequirementService {

    private final RequirementRepository requirementRepository;
    private final ProjectRepository projectRepository;

    public RequirementService(
            RequirementRepository requirementRepository,
            ProjectRepository projectRepository
    ) {
        this.requirementRepository = requirementRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional
    public Requirement createRequirement(
            String projectId,
            String title,
            String description,
            RequirementCategory category,
            String priority
    ) {
        Project project = findProject(projectId);

        Requirement requirement = new Requirement();
        requirement.setProject(project);
        requirement.setTitle(validateTitle(title));
        requirement.setDescription(description);
        requirement.setCategory(category);
        requirement.setPriority(validatePriority(priority));
        return requirementRepository.save(requirement);
    }

    public List<Requirement> getRequirements(String projectId) {
        findProject(projectId);
        return requirementRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    public Requirement getRequirement(String projectId, String requirementId) {
        findProject(projectId);
        return requirementRepository.findByIdAndProjectId(requirementId, projectId)
                .orElseThrow(() -> new RequirementNotFoundException(requirementId));
    }

    @Transactional
    public void deleteRequirement(String projectId, String requirementId) {
        Requirement requirement = getRequirement(projectId, requirementId);
        requirementRepository.delete(requirement);
    }

    private Project findProject(String projectId) {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("Project ID must not be blank");
        }
        return projectRepository.findById(projectId.trim())
                .orElseThrow(() -> new ProjectNotFoundException(projectId.trim()));
    }

    private String validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Requirement title must not be blank");
        }
        return title.trim();
    }

    private String validatePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            throw new IllegalArgumentException("Requirement priority must not be blank");
        }
        return priority.trim();
    }
}
