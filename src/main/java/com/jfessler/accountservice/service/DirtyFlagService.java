package com.jfessler.accountservice.service;

import com.jfessler.accountservice.model.DirtyFlag;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Slf4j
@Service
public class DirtyFlagService {

    private final DynamoDbTable<DirtyFlag> dirtyFlagTable;
    private final CircuitBreaker dirtyFlagCircuitBreaker;

    public DirtyFlagService(
            DynamoDbEnhancedClient enhancedClient,
            @Value("${aws.dynamodb.dirty-flag-table}") String tableName,
            @Qualifier("dirtyFlagCircuitBreaker") CircuitBreaker dirtyFlagCircuitBreaker) {
        this.dirtyFlagTable = enhancedClient.table(tableName, TableSchema.fromBean(DirtyFlag.class));
        this.dirtyFlagCircuitBreaker = dirtyFlagCircuitBreaker;
    }

    public boolean isDirty(UUID id) {
        try {
            return dirtyFlagCircuitBreaker.executeSupplier(() -> dirtyFlagTable.getItem(key(id)) != null);
        } catch (Exception e) {
            // In case of failure assume dirty rather than risk returning stale data from cache.
            log.warn("Dirty flag check failed for: {}", id, e);
            return true;
        }
    }

    public void markDirty(UUID id) {
        dirtyFlagTable.putItem(new DirtyFlag(id.toString(), getTtl()));
    }

    public void clearDirty(UUID id) {
        dirtyFlagTable.deleteItem(key(id));
    }

    private Key key(UUID id) {
        return Key.builder().partitionValue(id.toString()).build();
    }

    private long getTtl() {
        return Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond();
    }
}
