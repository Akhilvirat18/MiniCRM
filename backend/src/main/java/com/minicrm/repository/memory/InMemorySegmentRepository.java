package com.minicrm.repository.memory;

import com.minicrm.model.Segment;
import com.minicrm.repository.SegmentRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "crm.database.mode", havingValue = "in-memory", matchIfMissing = true)
public class InMemorySegmentRepository implements SegmentRepository {
    private final Map<String, Segment> store = new ConcurrentHashMap<>();

    @Override
    public Segment save(Segment segment) {
        if (segment.getId() == null) {
            segment.setId(UUID.randomUUID().toString());
        }
        store.put(segment.getId(), segment);
        return segment;
    }

    @Override
    public Optional<Segment> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Segment> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(String id) {
        store.remove(id);
    }

    @Override
    public long count() {
        return store.size();
    }
}
