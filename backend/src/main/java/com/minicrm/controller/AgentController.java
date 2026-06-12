package com.minicrm.controller;

import com.minicrm.model.AiAgentSession;
import com.minicrm.model.AiAuditLog;
import com.minicrm.model.Campaign;
import com.minicrm.repository.AiAgentSessionRepository;
import com.minicrm.repository.AiAuditLogRepository;
import com.minicrm.service.AgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/agent")
@CrossOrigin(origins = "${frontend.url}")
public class AgentController {

    private final AgentService agentService;
    private final AiAgentSessionRepository sessionRepository;
    private final AiAuditLogRepository auditLogRepository;

    public AgentController(AgentService agentService,
                           AiAgentSessionRepository sessionRepository,
                           AiAuditLogRepository auditLogRepository) {
        this.agentService = agentService;
        this.sessionRepository = sessionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @PostMapping("/campaign-plan")
    public ResponseEntity<AiAgentSession> initiateCampaignPlan(@RequestBody Map<String, Object> payload) {
        String goal = (String) payload.get("goal");
        
        if (goal == null || goal.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        double maxDiscount = 15.0; // default
        if (payload.containsKey("constraints")) {
            Map<String, Object> constraints = (Map<String, Object>) payload.get("constraints");
            if (constraints.containsKey("max_discount_percent")) {
                maxDiscount = Double.parseDouble(String.valueOf(constraints.get("max_discount_percent")));
            }
        }

        AiAgentSession session = agentService.initiatePlan(goal, maxDiscount);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/sessions/{id}/refine")
    public ResponseEntity<AiAgentSession> refineCampaignPlan(@PathVariable String id, @RequestBody Map<String, String> payload) {
        String editRequest = payload.get("goal"); // matches frontend payload structure
        if (editRequest == null || editRequest.trim().isEmpty()) {
            editRequest = payload.get("message");
        }
        
        if (editRequest == null || editRequest.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        AiAgentSession session = agentService.refinePlan(id, editRequest);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/sessions/{id}/approve")
    public ResponseEntity<Campaign> approveCampaignPlan(@PathVariable String id) {
        Campaign campaign = agentService.approvePlan(id);
        return ResponseEntity.ok(campaign);
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<AiAgentSession>> getAllSessions() {
        return ResponseEntity.ok(sessionRepository.findAll());
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<AiAgentSession> getSessionById(@PathVariable String id) {
        Optional<AiAgentSession> sess = sessionRepository.findById(id);
        return sess.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AiAuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditLogRepository.findAll());
    }
}
