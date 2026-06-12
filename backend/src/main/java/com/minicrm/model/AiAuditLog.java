package com.minicrm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "ai_audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAuditLog {
    @Id
    private String id;
    private String promptType; // e.g. segment_generation, message_generation, channel_recommendation, full_plan
    private String userPrompt;
    private String systemPrompt;
    private String rawResponse;
    private String parsedOutput; // JSON string of the structured result
    private String explanation;  // Human readable reasoning
    private LocalDateTime timestamp;
}
