package ai_frontend_factory.controller;

import ai_frontend_factory.entity.Project;
import ai_frontend_factory.entity.Requirement;
import ai_frontend_factory.entity.RequirementCategory;
import ai_frontend_factory.exception.GlobalExceptionHandler;
import ai_frontend_factory.exception.ProjectNotFoundException;
import ai_frontend_factory.exception.RequirementNotFoundException;
import ai_frontend_factory.service.RequirementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RequirementControllerTest {

    @Mock
    private RequirementService requirementService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RequirementController(requirementService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturns201() throws Exception {
        Requirement requirement = requirement("requirement-id", "project-id");
        when(requirementService.createRequirement(
                "project-id",
                "Authentication",
                "Use secure login",
                RequirementCategory.SECURITY,
                "HIGH"
        )).thenReturn(requirement);

        mockMvc.perform(post("/api/v1/projects/project-id/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Authentication",
                                  "description": "Use secure login",
                                  "category": "SECURITY",
                                  "priority": "HIGH"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("requirement-id"))
                .andExpect(jsonPath("$.projectId").value("project-id"))
                .andExpect(jsonPath("$.category").value("SECURITY"));
    }

    @Test
    void createWithMissingTitleReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/projects/project-id/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " ",
                                  "description": "Use secure login",
                                  "category": "SECURITY",
                                  "priority": "HIGH"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void getRequirementsReturns200() throws Exception {
        when(requirementService.getRequirements("project-id"))
                .thenReturn(List.of(requirement("requirement-id", "project-id")));

        mockMvc.perform(get("/api/v1/projects/project-id/requirements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("requirement-id"))
                .andExpect(jsonPath("$[0].projectId").value("project-id"));
    }

    @Test
    void getMissingRequirementReturns404() throws Exception {
        when(requirementService.getRequirement("project-id", "missing"))
                .thenThrow(new RequirementNotFoundException("missing"));

        mockMvc.perform(get("/api/v1/projects/project-id/requirements/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getRequirementsForMissingProjectReturns404() throws Exception {
        when(requirementService.getRequirements("missing"))
                .thenThrow(new ProjectNotFoundException("missing"));

        mockMvc.perform(get("/api/v1/projects/missing/requirements"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete(
                        "/api/v1/projects/project-id/requirements/requirement-id"))
                .andExpect(status().isNoContent());

        verify(requirementService)
                .deleteRequirement("project-id", "requirement-id");
    }

    private Requirement requirement(String requirementId, String projectId) {
        Project project = new Project();
        project.setId(projectId);
        project.setName("Factory");
        project.setStatus("CREATED");

        Requirement requirement = new Requirement();
        requirement.setId(requirementId);
        requirement.setProject(project);
        requirement.setTitle("Authentication");
        requirement.setDescription("Use secure login");
        requirement.setCategory(RequirementCategory.SECURITY);
        requirement.setPriority("HIGH");
        return requirement;
    }
}
