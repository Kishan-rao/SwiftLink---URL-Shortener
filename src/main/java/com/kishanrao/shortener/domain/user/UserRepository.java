package com.kishanrao.shortener.domain.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserRepository {

    private final DynamoDbEnhancedClient enhancedClient;

    private static final TableSchema<UserEntity> SCHEMA = TableSchema.fromBean(UserEntity.class);
    private static final String TABLE_NAME = "users";

    public void save(UserEntity user) {
        getTable().putItem(user);
    }

    public Optional<UserEntity> findById(String id) {
        return Optional.ofNullable(getTable().getItem(r -> r.key(k -> k.partitionValue(id))));
    }

    public Optional<UserEntity> findByEmail(String email) {
        DynamoDbIndex<UserEntity> emailIndex = getTable().index("email-index");
        var results = emailIndex.query(QueryConditional.keyEqualTo(k -> k.partitionValue(email)));
        return results.stream()
                .flatMap(page -> page.items().stream())
                .findFirst();
    }

    private DynamoDbTable<UserEntity> getTable() {
        return enhancedClient.table(TABLE_NAME, SCHEMA);
    }
}
