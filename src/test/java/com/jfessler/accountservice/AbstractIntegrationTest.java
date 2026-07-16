package com.jfessler.accountservice;

import java.net.URI;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

@Testcontainers
public class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:18");

    @Container
    static GenericContainer<?> dynamoDbContainer =
            new GenericContainer<>("amazon/dynamodb-local:2.6.1").withExposedPorts(8000);

    @DynamicPropertySource
    static void dynamoDbProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "aws.dynamodb.endpoint",
                () -> "http://" + dynamoDbContainer.getHost() + ":" + dynamoDbContainer.getMappedPort(8000));
        registry.add("aws.dynamodb.dirty-flag-table", () -> "dirty-flag-table-test");
    }

    @BeforeAll
    static void createDirtyFlagTable() {
        DynamoDbClient client = DynamoDbClient.builder()
                .endpointOverride(URI.create(
                        "http://" + dynamoDbContainer.getHost() + ":" + dynamoDbContainer.getMappedPort(8000)))
                .region(Region.US_EAST_1)
                .credentialsProvider(
                        StaticCredentialsProvider.create(AwsBasicCredentials.create("accessKey", "secretKey")))
                .build();

        client.createTable(CreateTableRequest.builder()
                .tableName("dirty-flag-table-test")
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .keySchema(KeySchemaElement.builder()
                        .attributeName("accountId")
                        .keyType(KeyType.HASH)
                        .build())
                .attributeDefinitions(AttributeDefinition.builder()
                        .attributeName("accountId")
                        .attributeType(ScalarAttributeType.S)
                        .build())
                .build());

        client.updateTimeToLive(UpdateTimeToLiveRequest.builder()
                .tableName("dirty-flag-table-test")
                .timeToLiveSpecification(TimeToLiveSpecification.builder()
                        .attributeName("ttl")
                        .enabled(true)
                        .build())
                .build());
    }
}
