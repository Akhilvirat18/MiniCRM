package com.minicrm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "communications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Communication {
    @Id
    private String id;
    private String campaignId;
    private String customerId;
    private String variantId; // e.g. "A" or "B"
    private String recipient; // e.g. email/phone
    private String personalizedMessage;
    private String status; // e.g. pending, sent, delivered, opened, clicked, failed
    private LocalDateTime updatedAt;
}
