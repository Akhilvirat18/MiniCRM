package com.minicrm.repository.mongo;

import com.minicrm.model.Segment;
import com.minicrm.repository.SegmentRepository;
import com.minicrm.repository.mongo.db.MongoSegmentDbRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "crm.database.mode", havingValue = "mongodb")
public class MongoSegmentRepositoryImpl implements SegmentRepository {
    
    private final MongoSegmentDbRepository db;
    
    public MongoSegmentRepositoryImpl(MongoSegmentDbRepository db) {
        this.db = db;
    }
    
    @Override
    public Segment save(Segment segment) {
        return db.save(segment);
    }
    
    @Override
    public Optional<Segment> findById(String id) {
        return db.findById(id);
    }
    
    @Override
    public List<Segment> findAll() {
        return db.findAll();
    }
    
    @Override
    public void deleteById(String id) {
        db.deleteById(id);
    }
    
    @Override
    public long count() {
        return db.count();
    }
}
