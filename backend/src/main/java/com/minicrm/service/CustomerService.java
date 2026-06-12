package com.minicrm.service;

import com.minicrm.model.Customer;
import com.minicrm.model.Order;
import com.minicrm.repository.CustomerRepository;
import com.minicrm.repository.OrderRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    public CustomerService(CustomerRepository customerRepository, OrderRepository orderRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Ingests a bulk list of customer maps.
     */
    public IngestionResult ingestCustomers(List<Map<String, Object>> rawCustomers) {
        IngestionResult result = new IngestionResult();
        int added = 0;
        int updated = 0;
        int duplicates = 0;

        for (int i = 0; i < rawCustomers.size(); i++) {
            Map<String, Object> raw = rawCustomers.get(i);
            int rowNum = i + 1;

            try {
                String name = (String) raw.get("name");
                String email = (String) raw.get("email");
                String phone = (String) raw.get("phone");
                String city = (String) raw.get("city");
                String tier = (String) raw.get("tier");
                Object consentObj = raw.get("consent");
                boolean consent = consentObj instanceof Boolean ? (Boolean) consentObj : Boolean.parseBoolean(String.valueOf(consentObj));

                // Basic validations
                if (name == null || name.trim().isEmpty()) {
                    result.addError(rowNum, "Name is required.");
                    continue;
                }
                if ((email == null || email.trim().isEmpty()) && (phone == null || phone.trim().isEmpty())) {
                    result.addError(rowNum, "At least email or phone number is required.");
                    continue;
                }

                // Check duplicates (email and phone deduplication)
                Optional<Customer> existingByEmail = email != null && !email.trim().isEmpty() ? customerRepository.findByEmail(email) : Optional.empty();
                Optional<Customer> existingByPhone = phone != null && !phone.trim().isEmpty() ? customerRepository.findByPhone(phone) : Optional.empty();

                Customer customerToSave;
                if (existingByEmail.isPresent()) {
                    customerToSave = existingByEmail.get();
                    updated++;
                } else if (existingByPhone.isPresent()) {
                    customerToSave = existingByPhone.get();
                    updated++;
                } else {
                    customerToSave = new Customer();
                    customerToSave.setCreatedAt(LocalDateTime.now());
                    added++;
                }

                customerToSave.setName(name);
                customerToSave.setEmail(email);
                customerToSave.setPhone(phone);
                customerToSave.setCity(city != null ? city : "Unknown");
                customerToSave.setTier(tier != null ? tier : "Bronze");
                customerToSave.setConsent(consent);

                customerRepository.save(customerToSave);

            } catch (Exception e) {
                result.addError(rowNum, "Ingestion fail: " + e.getMessage());
            }
        }

        result.setAddedCount(added);
        result.setUpdatedCount(updated);
        result.setDuplicateCount(duplicates);
        result.setSuccess(result.getErrors().isEmpty());
        return result;
    }

    /**
     * Ingests a bulk list of order maps.
     */
    public IngestionResult ingestOrders(List<Map<String, Object>> rawOrders) {
        IngestionResult result = new IngestionResult();
        int added = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        for (int i = 0; i < rawOrders.size(); i++) {
            Map<String, Object> raw = rawOrders.get(i);
            int rowNum = i + 1;

            try {
                String email = (String) raw.get("customer_email");
                String phone = (String) raw.get("customer_phone");
                Object amountObj = raw.get("amount");
                double amount = amountObj instanceof Number ? ((Number) amountObj).doubleValue() : Double.parseDouble(String.valueOf(amountObj));
                String channel = (String) raw.get("channel");
                String dateStr = (String) raw.get("order_date");
                List<Map<String, Object>> rawItems = (List<Map<String, Object>>) raw.get("items");

                LocalDateTime orderDate = LocalDateTime.now();
                if (dateStr != null && !dateStr.trim().isEmpty()) {
                    try {
                        orderDate = LocalDateTime.parse(dateStr, formatter);
                    } catch (Exception dtEx) {
                        // fallback to date parse without seconds or custom format
                        orderDate = LocalDateTime.parse(dateStr + "T00:00:00");
                    }
                }

                // Locate customer
                Optional<Customer> customerOpt = Optional.empty();
                if (email != null && !email.trim().isEmpty()) {
                    customerOpt = customerRepository.findByEmail(email);
                }
                if (customerOpt.isEmpty() && phone != null && !phone.trim().isEmpty()) {
                    customerOpt = customerRepository.findByPhone(phone);
                }

                if (customerOpt.isEmpty()) {
                    result.addError(rowNum, "Customer not found. Placeholders must be ingested first.");
                    continue;
                }

                List<Order.OrderItem> items = new ArrayList<>();
                if (rawItems != null) {
                    for (Map<String, Object> ri : rawItems) {
                        Object priceObj = ri.get("price");
                        double price = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : Double.parseDouble(String.valueOf(priceObj));
                        Object qtyObj = ri.get("quantity");
                        int qty = qtyObj instanceof Number ? ((Number) qtyObj).intValue() : Integer.parseInt(String.valueOf(qtyObj));

                        items.add(Order.OrderItem.builder()
                                .productId((String) ri.get("product_id"))
                                .category((String) ri.get("category"))
                                .quantity(qty)
                                .price(price)
                                .build());
                    }
                }

                Order order = Order.builder()
                        .customerId(customerOpt.get().getId())
                        .orderDate(orderDate)
                        .amount(amount)
                        .channel(channel != null ? channel : "online")
                        .items(items)
                        .build();

                orderRepository.save(order);
                added++;

            } catch (Exception e) {
                result.addError(rowNum, "Order ingestion fail: " + e.getMessage());
            }
        }

        result.setAddedCount(added);
        result.setSuccess(result.getErrors().isEmpty());
        return result;
    }

    @Data
    public static class IngestionResult {
        private boolean success;
        private int addedCount;
        private int updatedCount;
        private int duplicateCount;
        private List<IngestionError> errors = new ArrayList<>();

        public void addError(int row, String message) {
            errors.add(new IngestionError(row, message));
        }

        @Data
        public static class IngestionError {
            private final int row;
            private final String message;
        }
    }
}
