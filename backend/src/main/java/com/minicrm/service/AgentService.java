package com.minicrm.service;

import com.minicrm.model.AiAgentSession;
import com.minicrm.model.Campaign;
import com.minicrm.model.Customer;
import com.minicrm.model.Segment;
import com.minicrm.repository.AiAgentSessionRepository;
import com.minicrm.repository.CampaignRepository;
import com.minicrm.repository.SegmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class AgentService {

    private final AiAgentSessionRepository sessionRepository;
    private final CampaignRepository campaignRepository;
    private final SegmentRepository segmentRepository;
    private final LlmService llmService;
    private final SegmentService segmentService;

    public AgentService(AiAgentSessionRepository sessionRepository,
                        CampaignRepository campaignRepository,
                        SegmentRepository segmentRepository,
                        LlmService llmService,
                        SegmentService segmentService) {
        this.sessionRepository = sessionRepository;
        this.campaignRepository = campaignRepository;
        this.segmentRepository = segmentRepository;
        this.llmService = llmService;
        this.segmentService = segmentService;
    }

    /**
     * Creates a new session, calls the LLM planner, runs segment count dry-run, and builds explainability logs.
     */
    public AiAgentSession initiatePlan(String goal, double maxDiscount) {
        log.info("Initiating campaign plan for goal: \"{}\"", goal);

        // Call LLM generator
        Map<String, Object> planResult = llmService.generateCampaignPlan(goal, maxDiscount);

        // Extract plan and run preview query
        Map<String, Object> segmentFilter = (Map<String, Object>) planResult.get("segment_filter");
        List<Customer> matched = segmentService.evaluateFilter(segmentFilter);
        
        // Update projections with actual dry-run counts
        Map<String, Object> projections = (Map<String, Object>) planResult.get("projections");
        Map<String, Object> updatedProjections = new HashMap<>(projections);
        updatedProjections.put("expected_reach", matched.size());
        planResult.put("projections", updatedProjections);

        // Build session turns
        List<AiAgentSession.AgentTurn> turns = new ArrayList<>();
        turns.add(AiAgentSession.AgentTurn.builder()
                .role("user")
                .message(goal)
                .timestamp(LocalDateTime.now())
                .build());

        String agentMsg = String.format("I have analyzed your goal and proposed a plan targeting %d customers via %s. Please review the campaign proposal.",
                matched.size(), planResult.get("channel"));
        turns.add(AiAgentSession.AgentTurn.builder()
                .role("agent")
                .message(agentMsg)
                .timestamp(LocalDateTime.now())
                .build());

        // Extract explainability map
        Map<String, Object> explainability = (Map<String, Object>) planResult.get("explainability");

        AiAgentSession session = AiAgentSession.builder()
                .goal(goal)
                .turns(turns)
                .currentPlan(planResult)
                .explainability(explainability)
                .approved(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return sessionRepository.save(session);
    }

    /**
     * Submits a conversational edit instruction to adjust the proposed session plan.
     */
    public AiAgentSession refinePlan(String sessionId, String editRequest) {
        Optional<AiAgentSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        AiAgentSession session = sessionOpt.get();
        if (session.isApproved()) {
            throw new IllegalStateException("Cannot edit an approved campaign plan session.");
        }

        log.info("Refining session {} with edit: \"{}\"", sessionId, editRequest);

        // Append user turn
        session.getTurns().add(AiAgentSession.AgentTurn.builder()
                .role("user")
                .message(editRequest)
                .timestamp(LocalDateTime.now())
                .build());

        // Re-call LLM with previous details and edit request
        String fullGoal = String.format("Goal: %s. Previous Plan Message: %s. Request: %s",
                session.getGoal(),
                ((Map<String, Object>) session.getCurrentPlan().get("message")).get("template"),
                editRequest);

        Map<String, Object> planResult = llmService.generateCampaignPlan(fullGoal, 15.0);

        // Re-evaluate matching size
        Map<String, Object> segmentFilter = (Map<String, Object>) planResult.get("segment_filter");
        List<Customer> matched = segmentService.evaluateFilter(segmentFilter);
        
        Map<String, Object> projections = (Map<String, Object>) planResult.get("projections");
        Map<String, Object> updatedProjections = new HashMap<>(projections);
        updatedProjections.put("expected_reach", matched.size());
        planResult.put("projections", updatedProjections);

        // Update session state
        session.setCurrentPlan(planResult);
        session.setExplainability((Map<String, Object>) planResult.get("explainability"));
        session.setUpdatedAt(LocalDateTime.now());

        String agentMsg = String.format("Plan refined successfully! The updated segment has %d shoppers. Channel: %s.",
                matched.size(), planResult.get("channel"));
        session.getTurns().add(AiAgentSession.AgentTurn.builder()
                .role("agent")
                .message(agentMsg)
                .timestamp(LocalDateTime.now())
                .build());

        return sessionRepository.save(session);
    }

    /**
     * Approves the plan and materializes it into a working Segment and Campaign Draft in the DB.
     */
    public Campaign approvePlan(String sessionId) {
        Optional<AiAgentSession> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        AiAgentSession session = sessionOpt.get();
        if (session.isApproved()) {
            throw new IllegalStateException("Session is already approved.");
        }

        Map<String, Object> plan = session.getCurrentPlan();
        
        // 1. Create and Save the Segment
        String segmentName = (String) plan.get("segment_name");
        Map<String, Object> filter = (Map<String, Object>) plan.get("segment_filter");
        
        // Run final evaluation to record count
        List<Customer> matched = segmentService.evaluateFilter(filter);

        Segment segment = Segment.builder()
                .name(segmentName != null ? segmentName : "AI Segment - " + session.getGoal())
                .description(session.getGoal())
                .filterRules(filter)
                .previewCount(matched.size())
                .createdAt(LocalDateTime.now())
                .build();
        Segment savedSegment = segmentRepository.save(segment);

        // 2. Create the Campaign in status Draft
        String channel = (String) plan.get("channel");
        Map<String, Object> message = (Map<String, Object>) plan.get("message");
        String template = (String) message.get("template");

        List<Campaign.CampaignVariant> abVariants = new ArrayList<>();
        List<Map<String, String>> rawVariants = (List<Map<String, String>>) message.get("variants");
        if (rawVariants != null) {
            for (Map<String, String> rv : rawVariants) {
                abVariants.add(Campaign.CampaignVariant.builder()
                        .variantId(rv.get("variant_id"))
                        .template(rv.get("template"))
                        .build());
            }
        }

        Campaign campaign = Campaign.builder()
                .name("Campaign: " + session.getGoal())
                .segmentId(savedSegment.getId())
                .channel(channel != null ? channel.toLowerCase() : "sms")
                .status("Draft")
                .messageTemplate(template)
                .variants(abVariants)
                .agentSessionId(sessionId)
                .metrics(Campaign.CampaignMetrics.builder()
                        .sent(0)
                        .delivered(0)
                        .opened(0)
                        .clicked(0)
                        .failed(0)
                        .build())
                .createdAt(LocalDateTime.now())
                .build();
        Campaign savedCampaign = campaignRepository.save(campaign);

        // 3. Mark session as approved
        session.setApproved(true);
        session.setLinkedCampaignId(savedCampaign.getId());
        session.getTurns().add(AiAgentSession.AgentTurn.builder()
                .role("agent")
                .message("Fantastic! The campaign has been approved and saved as a draft. You can now trigger execution or adjust variants.")
                .timestamp(LocalDateTime.now())
                .build());
        sessionRepository.save(session);

        return savedCampaign;
    }
}
