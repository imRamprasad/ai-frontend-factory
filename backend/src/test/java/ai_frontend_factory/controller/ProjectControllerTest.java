package ai_frontend_factory.controller;

import ai_frontend_factory.entity.Project;
import ai_frontend_factory.exception.ProjectNotFoundException;
import ai_frontend_factory.exception.GlobalExceptionHandler;
import ai_frontend_factory.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    @Mock
    private ProjectService projectService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProjectController(projectService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturns201() throws Exception {
        Project project = project("project-id", "Factory");
        when(projectService.createProject("Factory", "Description")).thenReturn(project);

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Factory","description":"Description"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("project-id"))
                .andExpect(jsonPath("$.name").value("Factory"));
    }

    @Test
    void createWithBlankNameReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" ","description":"Description"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void getAllReturnsNewestFirstList() throws Exception {
        when(projectService.getAllProjectsNewestFirst())
                .thenReturn(List.of(project("one", "First"), project("two", "Second")));

        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("one"))
                .andExpect(jsonPath("$[1].id").value("two"));
    }

    @Test
    void getMissingProjectReturns404() throws Exception {
        when(projectService.getProjectById("missing"))
                .thenThrow(new ProjectNotFoundException("missing"));

        mockMvc.perform(get("/api/v1/projects/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Project not found: missing"));
    }

    @Test
    void updateReturns200() throws Exception {
        Project project = project("project-id", "Updated");
        when(projectService.updateProject("project-id", "Updated", "New description"))
                .thenReturn(project);

        mockMvc.perform(put("/api/v1/projects/project-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated","description":"New description"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/projects/project-id"))
                .andExpect(status().isNoContent());

        verify(projectService).deleteProject("project-id");
    }

    private Project project(String id, String name) {
        Project project = new Project();
        project.setId(id);
        project.setName(name);
        project.setDescription("Description");
        project.setStatus("CREATED");
        return project;
    }
}
