package com.minicrm.repository;

import com.minicrm.model.Customer;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository {
    Customer save(Customer customer);
    List<Customer> saveAll(List<Customer> customers);
    Optional<Customer> findById(String id);
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByPhone(String phone);
    List<Customer> findAll();
    long count();
    void deleteAll();
}
