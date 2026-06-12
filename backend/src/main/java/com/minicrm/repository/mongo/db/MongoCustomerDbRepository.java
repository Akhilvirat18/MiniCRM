package com.minicrm.repository.mongo.db;

import com.minicrm.model.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface MongoCustomerDbRepository extends MongoRepository<Customer, String> {
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByPhone(String phone);
}
