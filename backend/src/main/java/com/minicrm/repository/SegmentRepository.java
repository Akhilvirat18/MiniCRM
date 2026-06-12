package com.minicrm.repository;

import com.minicrm.model.Segment;
import java.util.List;
import java.util.Optional;

public interface SegmentRepository {
    Segment save(Segment segment);
    Optional<Segment> findById(String id);
    List<Segment> findAll();
    void deleteById(String id);
    long count();
}
