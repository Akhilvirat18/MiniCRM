package com.minicrm.repository.mongo.db;

import com.minicrm.model.Segment;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoSegmentDbRepository extends MongoRepository<Segment, String> {
}
