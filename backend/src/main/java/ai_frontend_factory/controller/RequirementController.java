package ai_frontend_factory.controller;

import ai_frontend_factory.dto.CreateRequirementRequest;
import ai_frontend_factory.dto.RequirementResponse;
import ai_frontend_factory.entity.Requirement;
import ai_frontend_factory.service.RequirementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/requirements")
public class RequirementController {

    private final RequirementService requirementService;

    public RequirementController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    @PostMapping
    public ResponseEntity<RequirementResponse> createRequirement(
            @PathVariable String projectId,
            @Valid @RequestBody CreateRequirementRequest request
    ) {
        Requirement requirement = requirementService.createRequirement(
                        projectId,
                        request.title(),
                        request.description(),
                        request.category(),
                        request.priority()
                );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RequirementResponse.from(requirement));
    }

    @GetMapping
    public List<RequirementResponse> getRequirements(@PathVariable String projectId) {
        return requirementService.getRequirements(projectId).stream()
                .map(RequirementResponse::from)
                .toList();
    }

    @GetMapping("/{requirementId}")
    public RequirementResponse getRequirement(
            @PathVariable String projectId,
            @PathVariable String requirementId
    ) {
        return RequirementResponse.from(
                requirementService.getRequirement(projectId, requirementId)
        );
    }

    @DeleteMapping("/{requirementId}")
    public ResponseEntity<Void> deleteRequirement(
            @PathVariable String projectId,
            @PathVariable String requirementId
    ) {
        requirementService.deleteRequirement(projectId, requirementId);
        return ResponseEntity.noContent().build();
    }
}
