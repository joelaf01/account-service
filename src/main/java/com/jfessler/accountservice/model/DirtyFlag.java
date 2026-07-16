package com.jfessler.accountservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@DynamoDbBean
public class DirtyFlag {

    private String accountId;

    @Getter
    private long ttl;

    @DynamoDbPartitionKey
    public String getAccountId() {
        return accountId;
    }
}
