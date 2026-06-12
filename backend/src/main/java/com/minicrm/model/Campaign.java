package com.minicrm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "campaigns")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Campaign {
    @Id
    private String id;
    private String name;
    private String segmentId;
    private String channel; // e.g. sms, email, whatsapp
    private String status; // e.g. Draft, Sending, Sent
    private String messageTemplate;
    private List<CampaignVariant> variants;
    private String agentSessionId;
    private CampaignMetrics metrics;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampaignVariant {
        private String variantId; // e.g. "A" or "B"
        private String template;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampaignMetrics {
        private int sent;
        private int delivered;
        private int read;        // WhatsApp/RCS read receipts
        private int opened;
        private int clicked;
        private int failed;
        private int conversions; // Orders attributed to this campaign
    }
}
