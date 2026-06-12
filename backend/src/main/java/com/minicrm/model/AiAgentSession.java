package com.minicrm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "ai_agent_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAgentSession {
    @Id
    private String id;
    private String goal;
    private List<AgentTurn> turns;
    private Map<String, Object> currentPlan; // Stores proposed plan details (segment, channel, copy templates, projections)
    private Map<String, Object> explainability; // Why segment, why channel, why message
    private boolean approved;
    private String linkedCampaignId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentTurn {
        private String role; // e.g. user, agent
        private String message;
        private LocalDateTime timestamp;
    }
}
