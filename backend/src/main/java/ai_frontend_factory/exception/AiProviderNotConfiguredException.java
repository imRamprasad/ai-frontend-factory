package ai_frontend_factory.exception;

public class AiProviderNotConfiguredException extends RuntimeException {

    public AiProviderNotConfiguredException() {
        super("No AI provider is configured. Configure and enable an AI provider adapter before running analysis.");
    }
}
