package ai_frontend_factory.provider;

import ai_frontend_factory.ai.AiProvider;
import ai_frontend_factory.ai.AiRequest;
import ai_frontend_factory.ai.AiResponse;
import ai_frontend_factory.ai.GeneratedRequirement;
import ai_frontend_factory.ai.RequirementPriority;
import ai_frontend_factory.entity.RequirementCategory;
import ai_frontend_factory.exception.AiProviderException;
import ai_frontend_factory.exception.AiProviderNotConfiguredException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.client.JdkClientHttpRequestFactory;

@Component
public class OpenRouterAiProvider implements AiProvider {

    private static final String DEFAULT_BASE_URL = "https://openrouter.ai/api/v1";

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final String apiKey;
    private final String model;

    @Autowired
    public OpenRouterAiProvider(
            @Value("${OPENROUTER_API_KEY:}") String apiKey,
            @Value("${OPENROUTER_MODEL:}") String model,
            @Value("${OPENROUTER_BASE_URL:" + DEFAULT_BASE_URL + "}")
            String baseUrl,
            @Value("${OPENROUTER_CONNECT_TIMEOUT:${openrouter.connect-timeout:5s}}")
            String connectTimeout,
            @Value("${OPENROUTER_READ_TIMEOUT:${openrouter.read-timeout:60s}}")
            String readTimeout
    ) {
        this(
                apiKey,
                model,
                baseUrl,
                parseDuration(connectTimeout, Duration.ofSeconds(5)),
                parseDuration(readTimeout, Duration.ofSeconds(60)),
                RestClient.builder()
        );
    }

    OpenRouterAiProvider(
            String apiKey,
            String model,
            String baseUrl,
            RestClient.Builder restClientBuilder
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null ? "" : model.trim();
        this.jsonMapper = JsonMapper.builder().build();

        String normalizedBaseUrl = normalizeBaseUrl(baseUrl);
        this.restClient = restClientBuilder
                .baseUrl(normalizedBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    OpenRouterAiProvider(
            String apiKey,
            String model,
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout,
            RestClient.Builder restClientBuilder
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null ? "" : model.trim();
        this.jsonMapper = JsonMapper.builder().build();

        String normalizedBaseUrl = normalizeBaseUrl(baseUrl);
        Duration safeConnectTimeout = positiveOrDefault(connectTimeout, Duration.ofSeconds(5));
        Duration safeReadTimeout = positiveOrDefault(readTimeout, Duration.ofSeconds(60));

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(safeConnectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(safeReadTimeout);

        this.restClient = restClientBuilder
                .baseUrl(normalizedBaseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public AiResponse generateRequirements(AiRequest request) {
        if (apiKey.isBlank()) {
            throw new AiProviderNotConfiguredException();
        }
        if (model.isBlank()) {
            throw new AiProviderNotConfiguredException();
        }
        if (request == null || request.description() == null
                || request.description().isBlank()) {
            throw new IllegalArgumentException("Project description must not be blank");
        }

        Map<String, Object> body = buildRequestBody(request.description().trim());

        String responseBody;
        try {
            responseBody = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException exception) {
            throw mapHttpError(exception);
        } catch (RestClientException exception) {
            throw new AiProviderException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI provider is temporarily unavailable"
            );
        }

        return parseResponse(responseBody);
    }

    private Map<String, Object> buildRequestBody(String description) {
        Map<String, Object> systemMessage = new LinkedHashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put(
                "content",
                "Extract concise software requirements. Return JSON only with "
                        + "\"summary\" and a \"requirements\" array. Each requirement "
                        + "must contain title, description, category, and priority. "
                        + "Category must be FUNCTIONAL, NON_FUNCTIONAL, TECHNICAL, "
                        + "DESIGN, or SECURITY. Priority must be HIGH, MEDIUM, or LOW."
        );

        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", description);

        Map<String, Object> jsonSchema = new LinkedHashMap<>();
        jsonSchema.put("name", "requirement_analysis");
        jsonSchema.put("strict", true);
        jsonSchema.put("schema", Map.of(
                "type", "object",
                "properties", Map.of(
                        "summary", Map.of("type", "string"),
                        "requirements", Map.of(
                                "type", "array",
                                "items", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "title", Map.of("type", "string"),
                                                "description", Map.of("type", "string"),
                                                "category", Map.of(
                                                        "type", "string",
                                                        "enum", List.of(
                                                                "FUNCTIONAL",
                                                                "NON_FUNCTIONAL",
                                                                "TECHNICAL",
                                                                "DESIGN",
                                                                "SECURITY"
                                                        )
                                                ),
                                                "priority", Map.of(
                                                        "type", "string",
                                                        "enum", List.of("HIGH", "MEDIUM", "LOW")
                                                )
                                        ),
                                        "required", List.of(
                                                "title", "description", "category", "priority"
                                        ),
                                        "additionalProperties", false
                                )
                        )
                ),
                "required", List.of("summary", "requirements"),
                "additionalProperties", false
        ));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(systemMessage, userMessage));
        body.put("temperature", 0.2);
        body.put("response_format", Map.of(
                "type", "json_schema",
                "json_schema", jsonSchema
        ));
        return body;
    }

    private AiResponse parseResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new AiProviderException(
                    HttpStatus.BAD_GATEWAY,
                    "AI provider returned an empty response"
            );
        }

        try {
            JsonNode root = jsonMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw malformed();
            }

            JsonNode content = choices.get(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()
                    || content.asText().isBlank()) {
                throw malformed();
            }

            JsonNode analysis = jsonMapper.readTree(content.asText());
            JsonNode summaryNode = analysis.path("summary");
            JsonNode requirementsNode = analysis.path("requirements");
            if (summaryNode.isMissingNode() || !summaryNode.isString()
                    || summaryNode.asText().isBlank()
                    || !requirementsNode.isArray()) {
                throw malformed();
            }

            List<GeneratedRequirement> requirements = new ArrayList<>();
            for (JsonNode node : requirementsNode) {
                String title = text(node, "title");
                String description = text(node, "description");
                String category = text(node, "category");
                String priority = text(node, "priority");

                if (title.isBlank() || description.isBlank()
                        || category.isBlank() || priority.isBlank()) {
                    throw malformed();
                }

                RequirementCategory requirementCategory;
                RequirementPriority requirementPriority;
                try {
                    requirementCategory = RequirementCategory.valueOf(category);
                    requirementPriority = RequirementPriority.valueOf(priority);
                } catch (IllegalArgumentException exception) {
                    throw malformed();
                }
                requirements.add(new GeneratedRequirement(
                        title,
                        description,
                        requirementCategory,
                        requirementPriority
                ));
            }

            if (requirements.isEmpty()) {
                throw malformed();
            }
            return new AiResponse(summaryNode.asText().trim(), requirements);
        } catch (AiProviderException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw malformed();
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText() : "";
    }

    private AiProviderException malformed() {
        return new AiProviderException(
                HttpStatus.BAD_GATEWAY,
                "AI provider returned malformed structured output"
        );
    }

    private AiProviderException mapHttpError(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        if (status == 401) {
            return new AiProviderException(
                    HttpStatus.BAD_GATEWAY,
                    "AI provider authentication failed"
            );
        }
        if (status == 402) {
            return new AiProviderException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "AI provider account has insufficient credits"
            );
        }
        if (status == 429) {
            return new AiProviderException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "AI provider rate limit exceeded"
            );
        }
        if (status >= 500) {
            return new AiProviderException(
                    HttpStatus.BAD_GATEWAY,
                    "AI provider is temporarily unavailable"
            );
        }
        if (status == 404) {
            return new AiProviderException(
                    HttpStatus.BAD_GATEWAY,
                    "Configured AI model or provider endpoint is unavailable"
            );
        }
        return new AiProviderException(
                HttpStatus.BAD_GATEWAY,
                "AI provider request failed"
        );
    }

    private static String normalizeBaseUrl(String baseUrl) {
        String value = baseUrl == null || baseUrl.isBlank()
                ? DEFAULT_BASE_URL
                : baseUrl.trim();
        value = value.replaceAll("/+$", "");
        if (value.endsWith("/chat/completions")) {
            value = value.substring(0, value.length() - "/chat/completions".length());
        }
        return value;
    }

    private static Duration positiveOrDefault(Duration value, Duration fallback) {
        return value != null && !value.isNegative() && !value.isZero()
                ? value
                : fallback;
    }

    private static Duration parseDuration(String value, Duration fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        String normalized = value.trim().toLowerCase();
        try {
            if (normalized.endsWith("ms")) {
                return positiveOrDefault(
                        Duration.ofMillis(Long.parseLong(normalized.substring(
                                0, normalized.length() - 2
                        ).trim())),
                        fallback
                );
            }
            if (normalized.endsWith("s")) {
                return positiveOrDefault(
                        Duration.ofSeconds(Long.parseLong(normalized.substring(
                                0, normalized.length() - 1
                        ).trim())),
                        fallback
                );
            }
            if (normalized.endsWith("m")) {
                return positiveOrDefault(
                        Duration.ofMinutes(Long.parseLong(normalized.substring(
                                0, normalized.length() - 1
                        ).trim())),
                        fallback
                );
            }
            if (normalized.startsWith("pt") || normalized.startsWith("p")) {
                return positiveOrDefault(Duration.parse(value.trim()), fallback);
            }
            return positiveOrDefault(
                    Duration.ofSeconds(Long.parseLong(normalized)),
                    fallback
            );
        } catch (RuntimeException exception) {
            return fallback;
        }
    }
}
