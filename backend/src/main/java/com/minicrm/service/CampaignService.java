package com.minicrm.service;

import com.minicrm.model.Campaign;
import com.minicrm.model.Communication;
import com.minicrm.model.Customer;
import com.minicrm.model.Order;
import com.minicrm.repository.CampaignRepository;
import com.minicrm.repository.CommunicationRepository;
import com.minicrm.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CommunicationRepository communicationRepository;
    private final SegmentService segmentService;
    private final StubChannelService stubChannelService;
    private final OrderRepository orderRepository;

    /**
     * Deploys a campaign: personalized message generation, consent filtering, A/B distribution, and dispatch queue.
     */
    public Campaign sendCampaign(String campaignId) {
        Optional<Campaign> campaignOpt = campaignRepository.findById(campaignId);
        if (campaignOpt.isEmpty()) {
            throw new IllegalArgumentException("Campaign not found: " + campaignId);
        }

        Campaign campaign = campaignOpt.get();
        if ("Sent".equalsIgnoreCase(campaign.getStatus()) || "Sending".equalsIgnoreCase(campaign.getStatus())) {
            throw new IllegalStateException("Campaign has already been sent.");
        }

        log.info("Starting dispatch for Campaign: {}", campaign.getName());
        campaign.setStatus("Sending");
        campaignRepository.save(campaign);

        // Fetch segment customers
        List<Customer> targetCustomers = segmentService.getSegmentCustomers(campaign.getSegmentId());

        // Fetch orders using targeted query instead of pulling all orders into memory.
        List<String> customerIds = targetCustomers.stream()
                .map(Customer::getId)
                .collect(java.util.stream.Collectors.toList());
        Map<String, List<Order>> ordersByCustomer = orderRepository.findByCustomerIdIn(customerIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(Order::getCustomerId));

        List<Communication> communications = new ArrayList<>();
        boolean isAbTest = campaign.getVariants() != null && campaign.getVariants().size() >= 2;
        int index = 0;

        for (Customer customer : targetCustomers) {
            // Respect consent exclusions
            if (!customer.isConsent()) {
                log.debug("Skipping customer {} due to false consent flag.", customer.getName());
                continue;
            }

            // Decide A/B variant allocation (deterministic 50/50 by index)
            String variantId = null;
            String selectedTemplate = campaign.getMessageTemplate();
            if (isAbTest) {
                Campaign.CampaignVariant variant = campaign.getVariants().get(index % 2);
                variantId = variant.getVariantId();
                selectedTemplate = variant.getTemplate();
                index++;
            }

            // Personalise using pre-loaded orders — no additional DB call per customer
            List<Order> customerOrders = ordersByCustomer.getOrDefault(customer.getId(), Collections.emptyList());
            String personalized = personalizeTemplate(selectedTemplate, customer, customerOrders);

            // Select contact detail based on channel type
            String recipient = customer.getEmail();
            if ("sms".equalsIgnoreCase(campaign.getChannel())
                    || "whatsapp".equalsIgnoreCase(campaign.getChannel())
                    || "rcs".equalsIgnoreCase(campaign.getChannel())) {
                recipient = customer.getPhone();
            }

            Communication comm = Communication.builder()
                    .campaignId(campaignId)
                    .customerId(customer.getId())
                    .variantId(variantId)
                    .recipient(recipient)
                    .personalizedMessage(personalized)
                    .status("pending")
                    .updatedAt(LocalDateTime.now())
                    .build();

            communications.add(comm);
        }

        // Save communication records
        List<Communication> savedComms = communicationRepository.saveAll(communications);

        // Trigger asynchronous channel sends
        for (Communication comm : savedComms) {
            stubChannelService.sendCommunication(comm);
        }

        campaign.setStatus("Sent");
        
        // Initialize cumulative metrics
        Campaign.CampaignMetrics initialMetrics = Campaign.CampaignMetrics.builder()
                .sent(savedComms.size())
                .delivered(0)
                .read(0)
                .opened(0)
                .clicked(0)
                .failed(0)
                .conversions(0)
                .build();
        campaign.setMetrics(initialMetrics);
        
        return campaignRepository.save(campaign);
    }

    /**
     * Re-aggregates and updates metrics on campaign level based on communication logs.
     */
    public void aggregateCampaignMetrics(String campaignId) {
        Optional<Campaign> campaignOpt = campaignRepository.findById(campaignId);
        if (campaignOpt.isEmpty()) return;

        Campaign campaign = campaignOpt.get();
        List<Communication> comms = communicationRepository.findByCampaignId(campaignId);

        int sent = comms.size();
        int delivered = 0;
        int read = 0;
        int opened = 0;
        int clicked = 0;
        int failed = 0;
        int conversions = 0;

        boolean isEmail = "email".equalsIgnoreCase(campaign.getChannel());

        for (Communication c : comms) {
            String status = c.getStatus().toLowerCase();
            // Each status implies all previous stages have been reached
            switch (status) {
                case "converted":
                    delivered++; clicked++; conversions++;
                    if (isEmail) opened++; else read++;
                    break;
                case "clicked":
                    delivered++; clicked++;
                    if (isEmail) opened++; else read++;
                    break;
                case "opened":
                    delivered++; opened++;
                    break;
                case "read":
                    delivered++; read++;
                    break;
                case "delivered":
                    delivered++;
                    break;
                case "failed":
                    failed++;
                    break;
                default:
                    // 'sent' or 'pending'
                    break;
            }
        }

        Campaign.CampaignMetrics metrics = Campaign.CampaignMetrics.builder()
                .sent(sent)
                .delivered(delivered)
                .read(read)
                .opened(opened)
                .clicked(clicked)
                .failed(failed)
                .conversions(conversions)
                .build();

        campaign.setMetrics(metrics);
        campaignRepository.save(campaign);
        log.debug("Campaign {} metrics updated: sent={}, delivered={}, read={}, opened={}, clicked={}, conversions={}",
                campaignId, sent, delivered, read, opened, clicked, conversions);
    }

    /**
     * Replaces personalisation tokens using a pre-loaded order list (no DB calls).
     * Supported tokens: {{name}}, {{city}}, {{tier}}, {{last_purchased_category}},
     *                   {{last_order_category}}, {{days_since_last_order}}
     */
    private String personalizeTemplate(String template, Customer customer, List<Order> customerOrders) {
        if (template == null) return "";

        String output = template;
        output = output.replace("{{name}}", customer.getName() != null ? customer.getName() : "Valued Customer");
        output = output.replace("{{city}}", customer.getCity() != null ? customer.getCity() : "your city");
        output = output.replace("{{tier}}", customer.getTier() != null ? customer.getTier() : "member");

        // Derive last-ordered category and days-since from the pre-loaded orders list
        String lastCat = "products";
        int daysSinceLastOrder = 9999;
        LocalDateTime latestOrderDate = null;

        for (Order o : customerOrders) {
            if (latestOrderDate == null || o.getOrderDate().isAfter(latestOrderDate)) {
                latestOrderDate = o.getOrderDate();
                if (o.getItems() != null && !o.getItems().isEmpty()) {
                    String cat = o.getItems().get(0).getCategory();
                    if (cat != null) lastCat = cat;
                }
            }
        }
        if (latestOrderDate != null) {
            daysSinceLastOrder = (int) java.time.Duration.between(latestOrderDate, LocalDateTime.now()).toDays();
            if (daysSinceLastOrder < 0) daysSinceLastOrder = 0;
        }

        output = output.replace("{{last_purchased_category}}", lastCat);
        output = output.replace("{{last_order_category}}", lastCat);
        output = output.replace("{{days_since_last_order}}", String.valueOf(daysSinceLastOrder));

        return output;
    }
}
