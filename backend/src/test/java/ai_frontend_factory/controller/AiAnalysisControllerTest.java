package ai_frontend_factory.controller;

import ai_frontend_factory.exception.AiProviderNotConfiguredException;
import ai_frontend_factory.exception.GlobalExceptionHandler;
import ai_frontend_factory.exception.ProjectNotFoundException;
import ai_frontend_factory.service.AiRequirementAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AiAnalysisControllerTest {

    @Mock
    private AiRequirementAnalyzer analyzer;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AiAnalysisController(analyzer))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void missingProviderReturns503() throws Exception {
        when(analyzer.analyze("project-id", "Build an e-commerce website"))
                .thenThrow(new AiProviderNotConfiguredException());

        mockMvc.perform(post("/api/v1/projects/project-id/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Build an e-commerce website"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value(
                        "No AI provider is configured. Configure and enable an AI provider adapter before running analysis."
                ));
    }

    @Test
    void missingProjectReturns404() throws Exception {
        when(analyzer.analyze("missing", "Build a website"))
                .thenThrow(new ProjectNotFoundException("missing"));

        mockMvc.perform(post("/api/v1/projects/missing/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Build a website"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void blankDescriptionReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/projects/project-id/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.description").exists());
    }
}
