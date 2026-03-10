package com.kishanrao.shortener.domain.url;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;

@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@DynamoDbBean
public class UrlEntity {

    private String code;
    @Getter private String originalUrl;
    @Getter private Long clicks;
    @Getter private Instant createdAt;
    @Getter private Instant expiresAt;   // null = never expires
    @Getter private String ownerId;      // null = anonymous
    @Getter private String alias;        // custom alias if set

    @DynamoDbPartitionKey
    public String getCode() {
        return code;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}
