package com.kishanrao.shortener.domain.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.time.Instant;

@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@DynamoDbBean
public class UserEntity {

    private String id;
    private String email;
    @Getter private String passwordHash;
    @Getter private String role;
    @Getter private Instant createdAt;

    // Explicit getters required so DynamoDB annotations are on the actual method
    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = "email-index")
    public String getEmail() {
        return email;
    }
}
