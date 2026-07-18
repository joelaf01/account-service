package com.jfessler.accountservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootApplication
public class AccountServiceApplication {

    public static void main(String[] args) {
        String secretArn = System.getenv("AWS_DB_SECRET_ARN");
        if (secretArn != null && !secretArn.isBlank()) {
            try (SecretsManagerClient client = SecretsManagerClient.builder().build()) {
                String secretJson = client.getSecretValue(GetSecretValueRequest.builder()
                                .secretId(secretArn)
                                .build())
                        .secretString();

                JsonNode secret = new ObjectMapper().readTree(secretJson);
                System.setProperty(
                        "spring.datasource.username", secret.get("username").asString());
                System.setProperty(
                        "spring.datasource.password", secret.get("password").asString());
            }
        }

        SpringApplication.run(AccountServiceApplication.class, args);
    }
}
