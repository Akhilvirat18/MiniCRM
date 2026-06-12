package com.minicrm.controller;

import com.minicrm.model.Campaign;
import com.minicrm.model.Communication;
import com.minicrm.repository.CampaignRepository;
import com.minicrm.repository.CommunicationRepository;
import com.minicrm.service.CampaignService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/campaigns")
@CrossOrigin(origins = "${frontend.url}")
public class CampaignController {

    private final CampaignRepository campaignRepository;
    private final CampaignService campaignService;
    private final CommunicationRepository communicationRepository;

    public CampaignController(CampaignRepository campaignRepository,
                              CampaignService campaignService,
                              CommunicationRepository communicationRepository) {
        this.campaignRepository = campaignRepository;
        this.campaignService = campaignService;
        this.communicationRepository = communicationRepository;
    }

    @GetMapping
    public ResponseEntity<List<Campaign>> getAllCampaigns() {
        return ResponseEntity.ok(campaignRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Campaign> getCampaignById(@PathVariable String id) {
        // Recalculate metrics on fetching to ensure real-time accuracy
        campaignService.aggregateCampaignMetrics(id);
        
        Optional<Campaign> campaign = campaignRepository.findById(id);
        return campaign.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Campaign> createCampaign(@RequestBody Campaign campaign) {
        campaign.setStatus("Draft");
        campaign.setCreatedAt(LocalDateTime.now());
        campaign.setMetrics(Campaign.CampaignMetrics.builder()
                .sent(0)
                .delivered(0)
                .opened(0)
                .clicked(0)
                .failed(0)
                .build());
        
        Campaign saved = campaignRepository.save(campaign);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<Campaign> sendCampaign(@PathVariable String id) {
        Campaign sentCampaign = campaignService.sendCampaign(id);
        return ResponseEntity.ok(sentCampaign);
    }

    @GetMapping("/{id}/communications")
    public ResponseEntity<List<Communication>> getCampaignCommunications(@PathVariable String id) {
        List<Communication> communications = communicationRepository.findByCampaignId(id);
        return ResponseEntity.ok(communications);
    }
}
