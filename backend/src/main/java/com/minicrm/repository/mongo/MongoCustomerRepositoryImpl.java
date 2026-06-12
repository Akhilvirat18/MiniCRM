package com.minicrm.repository.mongo;

import com.minicrm.model.Customer;
import com.minicrm.repository.CustomerRepository;
import com.minicrm.repository.mongo.db.MongoCustomerDbRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "crm.database.mode", havingValue = "mongodb")
public class MongoCustomerRepositoryImpl implements CustomerRepository {
    
    private final MongoCustomerDbRepository db;
    
    public MongoCustomerRepositoryImpl(MongoCustomerDbRepository db) {
        this.db = db;
    }
    
    @Override
    public Customer save(Customer customer) {
        return db.save(customer);
    }
    
    @Override
    public List<Customer> saveAll(List<Customer> customers) {
        return db.saveAll(customers);
    }
    
    @Override
    public Optional<Customer> findById(String id) {
        return db.findById(id);
    }
    
    @Override
    public Optional<Customer> findByEmail(String email) {
        return db.findByEmail(email);
    }
    
    @Override
    public Optional<Customer> findByPhone(String phone) {
        return db.findByPhone(phone);
    }
    
    @Override
    public List<Customer> findAll() {
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
