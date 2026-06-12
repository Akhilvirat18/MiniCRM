package com.minicrm.repository;

import com.minicrm.model.Order;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    List<Order> saveAll(List<Order> orders);
    Optional<Order> findById(String id);
    List<Order> findByCustomerId(String customerId);
    List<Order> findByCustomerIdIn(List<String> customerIds);
    List<Order> findAll();
    long count();
    void deleteAll();
}
