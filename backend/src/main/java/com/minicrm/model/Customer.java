package com.minicrm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "customers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    @Id
    private String id;
    private String name;
    private String email;
    private String phone;
    private String city;
    private String tier; // e.g. Gold, Silver, Bronze
    private boolean consent;
    private LocalDateTime createdAt;
}
