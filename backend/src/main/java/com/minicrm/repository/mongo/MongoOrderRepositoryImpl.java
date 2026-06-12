package com.minicrm.repository.mongo;

import com.minicrm.model.Order;
import com.minicrm.repository.OrderRepository;
import com.minicrm.repository.mongo.db.MongoOrderDbRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "crm.database.mode", havingValue = "mongodb")
public class MongoOrderRepositoryImpl implements OrderRepository {
    
    private final MongoOrderDbRepository db;
    
    public MongoOrderRepositoryImpl(MongoOrderDbRepository db) {
        this.db = db;
    }
    
    @Override
    public Order save(Order order) {
        return db.save(order);
    }
    
    @Override
    public List<Order> saveAll(List<Order> orders) {
        return db.saveAll(orders);
    }
    
    @Override
    public Optional<Order> findById(String id) {
        return db.findById(id);
    }
    
    @Override
    public List<Order> findByCustomerId(String customerId) {
        return db.findByCustomerId(customerId);
    }
    
    @Override
    public List<Order> findByCustomerIdIn(List<String> customerIds) {
        return db.findByCustomerIdIn(customerIds);
    }
    
    @Override
    public List<Order> findAll() {
        return db.findAll();
    }
    
    @Override
    public long count() {
        return db.count();
    }
    
    @Override
    public void deleteAll() {
        db.deleteAll();
    }
}
