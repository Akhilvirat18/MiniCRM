package com.minicrm.service;

import com.minicrm.model.Customer;
import com.minicrm.model.Order;
import com.minicrm.repository.CustomerRepository;
import com.minicrm.repository.OrderRepository;
import com.minicrm.repository.SegmentRepository;
import com.minicrm.repository.memory.InMemoryCustomerRepository;
import com.minicrm.repository.memory.InMemoryOrderRepository;
import com.minicrm.repository.memory.InMemorySegmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class SegmentServiceTest {

    private CustomerRepository customerRepository;
    private OrderRepository orderRepository;
    private SegmentRepository segmentRepository;
    private SegmentService segmentService;

    @BeforeEach
    public void setUp() {
        customerRepository = new InMemoryCustomerRepository();
        orderRepository = new InMemoryOrderRepository();
        segmentRepository = new InMemorySegmentRepository();
        segmentService = new SegmentService(customerRepository, orderRepository, segmentRepository);
    }

    @Test
    public void testEvaluateFilter_TierMatches() {
        Customer goldCustomer = Customer.builder()
                .name("Alice")
                .email("alice@example.com")
                .tier("Gold")
                .consent(true)
                .createdAt(LocalDateTime.now())
                .build();
        Customer silverCustomer = Customer.builder()
                .name("Bob")
                .email("bob@example.com")
                .tier("Silver")
                .consent(true)
                .createdAt(LocalDateTime.now())
                .build();

        customerRepository.save(goldCustomer);
        customerRepository.save(silverCustomer);

        Map<String, Object> rule = Map.of(
                "field", "tier",
                "op", "==",
                "value", "Gold"
        );
        Map<String, Object> filter = Map.of("and", List.of(rule));

        List<Customer> matched = segmentService.evaluateFilter(filter);
        assertEquals(1, matched.size());
        assertEquals("Alice", matched.get(0).getName());
    }

    @Test
    public void testEvaluateFilter_CategoryAffinity() {
        Customer customer = Customer.builder()
                .name("Charlie")
                .email("charlie@example.com")
                .tier("Bronze")
                .consent(true)
                .createdAt(LocalDateTime.now())
                .build();
        customer = customerRepository.save(customer);

        Order order = Order.builder()
                .customerId(customer.getId())
                .orderDate(LocalDateTime.now().minusDays(10))
                .amount(100.0)
                .items(List.of(Order.OrderItem.builder()
                        .productId("prod_1")
                        .category("Coffee Beans")
                        .quantity(2)
                        .price(50.0)
                        .build()))
                .build();
        orderRepository.save(order);

        Map<String, Object> rule = Map.of(
                "field", "category_affinity",
                "op", "contains",
                "value", "Coffee Beans"
        );
        Map<String, Object> filter = Map.of("and", List.of(rule));

        List<Customer> matched = segmentService.evaluateFilter(filter);
        assertEquals(1, matched.size());
        assertEquals("Charlie", matched.get(0).getName());
    }
}
