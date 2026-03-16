package com.kishanrao.shortener.integration;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Skip the test (or test class) when Docker is not available for Testcontainers.
 * On Windows, requires "Expose daemon on tcp://localhost:2375" in Docker Desktop.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(EnabledIfDockerAvailableCondition.class)
public @interface EnabledIfDockerAvailable {
}
