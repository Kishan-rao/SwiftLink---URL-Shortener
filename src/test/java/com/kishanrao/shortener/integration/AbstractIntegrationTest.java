package com.kishanrao.shortener.integration;

import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@AutoConfigureWebTestClient
public abstract class AbstractIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    protected static final GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:alpine"))
                    .withExposedPorts(6379)
                    .waitingFor(Wait.forListeningPort());

    @Container
    @SuppressWarnings("resource")
    protected static final GenericContainer<?> dynamo = new GenericContainer<>(
            DockerImageName.parse("amazon/dynamodb-local:latest"))
                    .withExposedPorts(8000)
                    .withCommand("-jar DynamoDBLocal.jar -sharedDb")
                    .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    protected static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);

        registry.add("aws.region", () -> "us-east-1");
        registry.add("aws.dynamodb.endpoint", () -> "http://" + dynamo.getHost() + ":" + dynamo.getFirstMappedPort());
        registry.add("aws.dynamo.access-key", () -> "fakeKey");
        registry.add("aws.dynamo.secret-key", () -> "fakeSecret");
    }
}
