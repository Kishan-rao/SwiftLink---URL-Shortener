package com.kishanrao.shortener.infra.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import com.kishanrao.shortener.domain.url.UrlEntity;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DynamoDbInitializer {

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbClient dynamoDbClient;

    @PostConstruct
    public void setupDatabase() {
        createUrlsTable();
        createUsersTable();
    }

    private void createUrlsTable() {
        try {
            var table = enhancedClient.table("urls", TableSchema.fromBean(UrlEntity.class));
            table.createTable();
            log.info("Created 'urls' table.");
        } catch (ResourceInUseException e) {
            log.info("Table 'urls' already exists. Skipping.");
        }
    }

    private void createUsersTable() {
        try {
            // Use low-level client to create the 'users' table with a GSI on email.
            dynamoDbClient.createTable(r -> r
                    .tableName("users")
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("id").attributeType(ScalarAttributeType.S).build(),
                            AttributeDefinition.builder()
                                    .attributeName("email").attributeType(ScalarAttributeType.S).build()
                    )
                    .keySchema(
                            KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build()
                    )
                    .globalSecondaryIndexes(GlobalSecondaryIndex.builder()
                            .indexName("email-index")
                            .keySchema(KeySchemaElement.builder()
                                    .attributeName("email").keyType(KeyType.HASH).build())
                            .projection(p -> p.projectionType(ProjectionType.ALL))
                            .build()
                    )
            );
            log.info("Created 'users' table with email-index GSI.");
        } catch (ResourceInUseException e) {
            log.info("Table 'users' already exists. Skipping.");
        }
    }
}
