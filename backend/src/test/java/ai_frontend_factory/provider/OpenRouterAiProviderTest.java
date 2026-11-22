package ai_frontend_factory.provider;

import ai_frontend_factory.ai.AiRequest;
import ai_frontend_factory.ai.AiResponse;
import ai_frontend_factory.ai.RequirementPriority;
import ai_frontend_factory.entity.RequirementCategory;
import ai_frontend_factory.exception.AiProviderException;
import ai_frontend_factory.exception.AiProviderNotConfiguredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenRouterAiProviderTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    @Test
    void successfulResponseIsMappedToAiResponse() {
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess(successJson(), MediaType.APPLICATION_JSON));

        OpenRouterAiProvider provider = new OpenRouterAiProvider(
                "test-key",
                "test/model",
                "https://openrouter.ai/api/v1",
                builder
        );

        AiResponse response = provider.generateRequirements(
                new AiRequest("Build an e-commerce website")
        );

        assertEquals("E-commerce platform", response.summary());
        assertEquals(2, response.requirements().size());
        assertEquals("User login", response.requirements().getFirst().title());
        assertEquals(
                RequirementCategory.SECURITY,
                response.requirements().getFirst().category()
        );
        assertEquals(
                RequirementPriority.HIGH,
                response.requirements().getFirst().priority()
        );
        server.verify();
    }

    @Test
    void malformedJsonIsRejected() {
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andRespond(withSuccess("{not-json}", MediaType.APPLICATION_JSON));
        OpenRouterAiProvider provider = configuredProvider();

        AiProviderException exception = assertThrows(
                AiProviderException.class,
                () -> provider.generateRequirements(new AiRequest("Build a website"))
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        server.verify();
    }

    @Test
    void missingApiKeyIsRejected() {
        OpenRouterAiProvider provider = new OpenRouterAiProvider(
                "",
                "test/model",
                "https://openrouter.ai/api/v1",
                builder
        );

        assertThrows(
                AiProviderNotConfiguredException.class,
                () -> provider.generateRequirements(new AiRequest("Build a website"))
        );
    }

    @Test
    void missingModelIsRejected() {
        OpenRouterAiProvider provider = new OpenRouterAiProvider(
                "test-key",
                "",
                "https://openrouter.ai/api/v1",
                builder
        );

        assertThrows(
                AiProviderNotConfiguredException.class,
                () -> provider.generateRequirements(new AiRequest("Build a website"))
        );
    }

    @Test
    void unauthorizedResponseIsMappedSafely() {
        assertHttpStatus(HttpStatus.UNAUTHORIZED, HttpStatus.BAD_GATEWAY);
    }

    @Test
    void insufficientCreditsResponseIsMappedSafely() {
        assertHttpStatus(HttpStatus.PAYMENT_REQUIRED, HttpStatus.PAYMENT_REQUIRED);
    }

    @Test
    void rateLimitResponseIsMappedSafely() {
        assertHttpStatus(HttpStatus.TOO_MANY_REQUESTS, HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void serverErrorResponseIsMappedSafely() {
        assertHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY);
    }

    private void assertHttpStatus(HttpStatus providerStatus, HttpStatus expectedStatus) {
        server.expect(requestTo("https://openrouter.ai/api/v1/chat/completions"))
                .andRespond(withStatus(providerStatus)
                        .body("{\"error\":{\"message\":\"secret provider detail\"}}")
                        .contentType(MediaType.APPLICATION_JSON));
        OpenRouterAiProvider provider = configuredProvider();

        AiProviderException exception = assertThrows(
                AiProviderException.class,
                () -> provider.generateRequirements(new AiRequest("Build a website"))
        );

        assertEquals(expectedStatus, exception.getStatus());
        server.verify();
    }

    private OpenRouterAiProvider configuredProvider() {
        return new OpenRouterAiProvider(
                "test-key",
                "test/model",
                "https://openrouter.ai/api/v1",
                builder
        );
    }

    private String successJson() {
        return """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"summary\\":\\"E-commerce platform\\",\\"requirements\\":[{\\"title\\":\\"User login\\",\\"description\\":\\"Allow secure account access\\",\\"category\\":\\"SECURITY\\",\\"priority\\":\\"HIGH\\"},{\\"title\\":\\"Product management\\",\\"description\\":\\"Manage product catalog\\",\\"category\\":\\"FUNCTIONAL\\",\\"priority\\":\\"MEDIUM\\"}]}"
                      }
                    }
                  ]
                }
                """;
    }
}
