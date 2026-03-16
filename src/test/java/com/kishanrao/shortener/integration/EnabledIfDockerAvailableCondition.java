package com.kishanrao.shortener.integration;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 condition: enable test only when Docker is available for Testcontainers.
 */
public class EnabledIfDockerAvailableCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (DockerAssumptions.isDockerAvailable()) {
            return ConditionEvaluationResult.enabled("Docker is available");
        }
        return ConditionEvaluationResult.disabled(
                "Docker not available for Testcontainers (e.g. on Windows enable 'Expose daemon on tcp://localhost:2375' in Docker Desktop)");
    }
}
