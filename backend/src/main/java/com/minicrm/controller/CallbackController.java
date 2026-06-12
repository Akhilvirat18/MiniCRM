package com.minicrm.controller;

import com.minicrm.model.Communication;
import com.minicrm.repository.CommunicationRepository;
import com.minicrm.service.CampaignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/callbacks")
@CrossOrigin(origins = "${frontend.url}")
@Slf4j
public class CallbackController {

    private final CommunicationRepository communicationRepository;
    private final CampaignService campaignService;

    public CallbackController(CommunicationRepository communicationRepository, CampaignService campaignService) {
        this.communicationRepository = communicationRepository;
        this.campaignService = campaignService;
    }

    @PostMapping("/delivery")
    public ResponseEntity<Map<String, Object>> handleDeliveryCallback(@RequestBody Map<String, String> payload) {
        String commId = payload.get("communication_id");
        String event = payload.get("event");

        Map<String, Object> response = new HashMap<>();

        if (commId == null || event == null) {
            response.put("status", "error");
            response.put("message", "Missing communication_id or event");
            return ResponseEntity.badRequest().body(response);
        }

        Optional<Communication> commOpt = communicationRepository.findById(commId);
        if (commOpt.isEmpty()) {
            response.put("status", "error");
            response.put("message", "Communication record not found: " + commId);
            return ResponseEntity.status(404).body(response);
        }

        Communication comm = commOpt.get();
        String currentStatus = comm.getStatus();

        // Idempotency check: only transition status upwards in priority:
        // pending -> sent -> delivered -> opened -> clicked
        // failed can happen from pending/sent
        if (shouldUpdateStatus(currentStatus, event)) {
            comm.setStatus(event);
            comm.setUpdatedAt(LocalDateTime.now());
            communicationRepository.save(comm);
            
            // Re-aggregate campaign metrics dynamically
            campaignService.aggregateCampaignMetrics(comm.getCampaignId());
            
            log.info("Callback updated Communication {} status to {}", commId, event);
            response.put("status", "success");
            response.put("updated", true);
        } else {
            log.info("Idempotency match: Communication {} already at status {}, ignored incoming event {}", 
                    commId, currentStatus, event);
            response.put("status", "success");
            response.put("updated", false);
            response.put("message", "No state transition required.");
        }

        return ResponseEntity.ok(response);
    }

    private boolean shouldUpdateStatus(String current, String incoming) {
        if (current == null) return true;
        
        int currentWeight = getStatusWeight(current);
        int incomingWeight = getStatusWeight(incoming);
        
        // Update if incoming state has higher progression priority
        return incomingWeight > currentWeight;
    }

    private int getStatusWeight(String status) {
        switch (status.toLowerCase()) {
            case "pending":   return 0;
            case "sent":      return 1;
            case "failed":    return 2;
            case "delivered": return 3;
            case "read":      return 4; // WhatsApp/RCS read receipt
            case "opened":    return 5;
            case "clicked":   return 6;
            case "converted": return 7; // Order attributed to this message
            default:          return -1;
        }
    }
}
