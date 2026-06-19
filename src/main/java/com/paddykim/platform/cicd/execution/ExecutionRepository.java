package com.paddykim.platform.cicd.execution;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class ExecutionRepository {

    private final AtomicLong idSequence = new AtomicLong(1);
    private final ConcurrentMap<Long, ExecutionRecord> records = new ConcurrentHashMap<>();

    public ExecutionRecord save(ExecutionCreateRequest request) {
        Long id = idSequence.getAndIncrement();
        ExecutionRecord record = new ExecutionRecord(
                id,
                request.portalRequestId(),
                request.applicationName().trim(),
                request.environment().trim(),
                request.componentName().trim(),
                request.requestType(),
                request.requestedValue().trim(),
                request.requestedBy().trim(),
                ExecutionStatus.REQUESTED,
                "Execution request received by platform-cicd"
        );
        records.put(id, record);

        return record;
    }

    public Optional<ExecutionRecord> findById(Long id) {
        return Optional.ofNullable(records.get(id));
    }

    public List<ExecutionRecord> findAll() {
        return records.values().stream()
                .sorted(Comparator.comparing(ExecutionRecord::getCreatedAt).reversed())
                .toList();
    }

    public void deleteAll() {
        records.clear();
        idSequence.set(1);
    }
}
