package ai_frontend_factory.provider;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenRouterAiProviderContextTest {

    @Test
    void providerBeanCanBeCreatedFromApplicationContext() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("test-openrouter", Map.of(
                            "OPENROUTER_API_KEY", "test-key",
                            "OPENROUTER_MODEL", "test/model",
                            "OPENROUTER_BASE_URL", "https://openrouter.ai/api/v1",
                            "openrouter.connect-timeout", "5s",
                            "openrouter.read-timeout", "60s"
                    ))
            );
            context.register(OpenRouterAiProvider.class);
            context.refresh();

            OpenRouterAiProvider provider =
                    context.getBean(OpenRouterAiProvider.class);
            assertNotNull(provider);
        }
    }
}
