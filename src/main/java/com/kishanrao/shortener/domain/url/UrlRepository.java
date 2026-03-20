package com.kishanrao.shortener.domain.url;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UrlRepository {

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbClient dynamoDbClient;

    private static final TableSchema<UrlEntity> SCHEMA = TableSchema.fromBean(UrlEntity.class);
    private static final String TABLE_NAME = "urls";

    public void save(UrlEntity entity) {
        getTable().putItem(entity);
    }

    public Optional<UrlEntity> findById(String code) {
        var entity = getTable().getItem(r -> r.key(k -> k.partitionValue(code)));
        return Optional.ofNullable(entity);
    }

    public void updateClickCount(String code, long totalClicks) {
        dynamoDbClient.updateItem(req -> req
                .tableName(TABLE_NAME)
                .key(Map.of("code", AttributeValue.builder().s(code).build()))
                .updateExpression("ADD clicks :inc")
                .expressionAttributeValues(Map.of(":inc", AttributeValue.builder().n(String.valueOf(totalClicks)).build()))
        );
    }

    /**
     * Returns all URLs owned by a given user.
     * NOTE: This uses a full table scan with a filter expression.
     * For production scale, add a GSI on ownerId.
     */
    public List<UrlEntity> findByOwnerId(String ownerId) {
        var filterExpr = Expression.builder()
                .expression("ownerId = :ownerId")
                .expressionValues(Map.of(":ownerId", AttributeValue.builder().s(ownerId).build()))
                .build();

        var scanRequest = ScanEnhancedRequest.builder()
                .filterExpression(filterExpr)
                .build();

        return getTable().scan(scanRequest).items().stream().toList();
    }

    public void delete(String code) {
        getTable().deleteItem(r -> r.key(k -> k.partitionValue(code)));
    }

    private DynamoDbTable<UrlEntity> getTable() {
        return enhancedClient.table(TABLE_NAME, SCHEMA);
    }
}
