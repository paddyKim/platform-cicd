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

    public Long nextId() {
        return idSequence.getAndIncrement();
    }

    public ExecutionRecord save(ExecutionRecord record) {
        records.put(record.getId(), record);

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
