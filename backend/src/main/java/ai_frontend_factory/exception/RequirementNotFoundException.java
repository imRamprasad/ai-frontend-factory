package ai_frontend_factory.exception;

public class RequirementNotFoundException extends RuntimeException {

    public RequirementNotFoundException(String requirementId) {
        super("Requirement not found: " + requirementId);
    }
}
