package com.minicrm.repository.mongo.db;

import com.minicrm.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface MongoOrderDbRepository extends MongoRepository<Order, String> {
    List<Order> findByCustomerId(String customerId);
    List<Order> findByCustomerIdIn(List<String> customerIds);
}
