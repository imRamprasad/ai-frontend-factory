package ai_frontend_factory.controller;

import ai_frontend_factory.ai.AiResponse;
import ai_frontend_factory.dto.AiAnalysisResponse;
import ai_frontend_factory.dto.AnalyzeProjectRequest;
import ai_frontend_factory.dto.RequirementResponse;
import ai_frontend_factory.service.AiRequirementAnalyzer;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/analyze")
public class AiAnalysisController {

    private final AiRequirementAnalyzer analyzer;

    public AiAnalysisController(AiRequirementAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    @PostMapping
    public AiAnalysisResponse analyze(
            @PathVariable String projectId,
            @Valid @RequestBody AnalyzeProjectRequest request
    ) {
        AiResponse response = analyzer.analyze(projectId, request.description());
        List<RequirementResponse> requirements =
                    analyzer.persistAnalysis(projectId, response);
        return AiAnalysisResponse.from(
                projectId,
                response.summary(),
                requirements
        );
    }
}
