package com.minicrm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    private String id;
    private String customerId;
    private LocalDateTime orderDate;
    private double amount;
    private List<OrderItem> items;
    private String channel; // e.g. online, retail

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private String productId;
        private String category; // e.g. Coffee Beans, Filters
        private int quantity;
        private double price;
    }
}
