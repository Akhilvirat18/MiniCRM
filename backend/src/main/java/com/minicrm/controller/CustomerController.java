package com.minicrm.controller;

import com.minicrm.model.Customer;
import com.minicrm.model.Order;
import com.minicrm.repository.CustomerRepository;
import com.minicrm.repository.OrderRepository;
import com.minicrm.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "${frontend.url}")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    public CustomerController(CustomerService customerService,
                              CustomerRepository customerRepository,
                              OrderRepository orderRepository) {
        this.customerService = customerService;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
    }

    @PostMapping("/customers/ingest")
    public ResponseEntity<CustomerService.IngestionResult> ingestCustomers(@RequestBody List<Map<String, Object>> rawCustomers) {
        CustomerService.IngestionResult result = customerService.ingestCustomers(rawCustomers);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/orders/ingest")
    public ResponseEntity<CustomerService.IngestionResult> ingestOrders(@RequestBody List<Map<String, Object>> rawOrders) {
        CustomerService.IngestionResult result = customerService.ingestOrders(rawOrders);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/customers")
    public ResponseEntity<List<Customer>> getAllCustomers() {
        return ResponseEntity.ok(customerRepository.findAll());
    }

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderRepository.findAll());
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetDatabase() {
        customerRepository.deleteAll();
        orderRepository.deleteAll();
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Database reset completed successfully.");
        return ResponseEntity.ok(resp);
    }
}
