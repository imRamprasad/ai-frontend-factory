package ai_frontend_factory.ai;

import java.util.List;

public record AiResponse(
        String summary,
        List<GeneratedRequirement> requirements
) {
}
