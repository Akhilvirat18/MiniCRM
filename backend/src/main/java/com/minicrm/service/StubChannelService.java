package com.minicrm.service;

import com.minicrm.model.Communication;
import com.minicrm.repository.CommunicationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * StubChannelService simulates an external messaging channel provider.
 *
 * Design: The CRM calls sendCommunication() which asynchronously schedules
 * loopback HTTP callbacks to /api/callbacks/delivery — mirroring exactly how
 * real providers (Twilio, SendGrid, WhatsApp BSP) push delivery receipts back
 * to a CRM via webhooks.
 *
 * All event probabilities and retry policy are configurable via application.properties
 * (stub.* keys), so the stub can model different channel characteristics without
 * any code changes — e.g. WhatsApp has higher read rates, email has lower open rates.
 *
 * Scale note: At production volume (>100k sends/campaign) this would be replaced
 * with a Kafka topic per channel, consumed by isolated channel workers. The
 * ScheduledExecutorService here is intentionally scoped to demo-scale.
 */
@Service
@Slf4j
public class StubChannelService {

    // --- Retry Policy (configurable) ---
    @Value("${stub.callback.max-retries:3}")
    private int maxCallbackRetries;

    @Value("${stub.callback.retry-base-delay-ms:100}")
    private long retryBaseDelayMs;

    // --- Simulated Event Rates (configurable) ---
    @Value("${stub.read-rate:0.60}")
    private double readRate;

    @Value("${stub.open-rate:0.35}")
    private double openRate;

    @Value("${stub.click-rate:0.15}")
    private double clickRate;

    @Value("${stub.convert-rate:0.10}")
    private double convertRate;

    @Value("${stub.failure-rate:0.05}")
    private double failureRate;

    private final CommunicationRepository communicationRepository;
    private final ScheduledExecutorService scheduler;
    private final RestTemplate restTemplate;
    private final Random random = new Random();

    @Value("${crm.callback.url:http://localhost:8080/api/callbacks/delivery}")
    private String callbackUrl;

    public StubChannelService(CommunicationRepository communicationRepository) {
        this.communicationRepository = communicationRepository;
        this.scheduler = Executors.newScheduledThreadPool(4);
        this.restTemplate = new RestTemplate();
    }

    /**
     * Queues a communication for simulated async dispatch and triggers callback events.
     * Marked @Async so the calling HTTP thread returns immediately — the campaign send
     * API responds before all communications are dispatched.
     *
     * Scale tradeoff: For very large segments (>50k), this would move to a work queue
     * (e.g. Kafka topic) with back-pressure control rather than saturating the thread pool.
     */
    @Async
    public void sendCommunication(Communication communication) {
        log.info("Queueing message dispatch for Communication ID: {}, Recipient: {}, Channel: {}",
                communication.getId(), communication.getRecipient(),
                communication.getCampaignId());

        // Mark as sent immediately
        communication.setStatus("sent");
        communicationRepository.save(communication);

        // --- Delivery (always fires, 1–2s delay) ---
        long deliveryDelay = 1000 + random.nextInt(1000);
        scheduler.schedule(() -> triggerCallbackWithRetry(communication.getId(), "delivered"),
                deliveryDelay, TimeUnit.MILLISECONDS);

        // --- Engagement event probabilities (all configurable via application.properties) ---
        // read: WhatsApp blue-tick / RCS read receipt
        // opened: email tracking pixel fire
        // clicked: link click (only if opened)
        // converted: order placed after clicking (only if clicked)

        // Schedule Read (simulates WhatsApp/RCS read receipt)
        if (random.nextDouble() < readRate) {
            long readDelay = deliveryDelay + 500 + random.nextInt(2500);
            scheduler.schedule(() -> triggerCallbackWithRetry(communication.getId(), "read"),
                    readDelay, TimeUnit.MILLISECONDS);
        }

        // Schedule Open (email open pixel)
        if (random.nextDouble() < openRate) {
            long openDelay = 3000 + random.nextInt(4000);
            scheduler.schedule(() -> triggerCallbackWithRetry(communication.getId(), "opened"),
                    openDelay, TimeUnit.MILLISECONDS);

            // Schedule Click (only if opened)
            if (random.nextDouble() < clickRate) {
                long clickDelay = openDelay + 2000 + random.nextInt(4000);
                scheduler.schedule(() -> triggerCallbackWithRetry(communication.getId(), "clicked"),
                        clickDelay, TimeUnit.MILLISECONDS);

                // Schedule Conversion — order attributed to this campaign message
                if (random.nextDouble() < convertRate) {
                    long convertDelay = clickDelay + 5000 + random.nextInt(10000);
                    scheduler.schedule(() -> triggerCallbackWithRetry(communication.getId(), "converted"),
                            convertDelay, TimeUnit.MILLISECONDS);
                }
            }
        } else {
            // Small fraction of failed sends
            if (random.nextDouble() < failureRate) {
                scheduler.schedule(() -> triggerCallbackWithRetry(communication.getId(), "failed"),
                        2000 + random.nextInt(1000), TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * Fires a loopback HTTP POST to the CRM receipt API, with exponential backoff retry.
     *
     * Retry strategy: up to maxCallbackRetries attempts (configurable via stub.callback.max-retries).
     *   Attempt 1: immediate
     *   Attempt 2: retryBaseDelayMs (default 100ms)
     *   Attempt 3: retryBaseDelayMs * 2 (default 200ms)
     * On total failure: falls back to direct DB update to avoid losing the event.
     *
     * Production note: a real provider would use a durable outbox or SQS DLQ
     * for guaranteed delivery instead of in-process retry.
     */
    private void triggerCallbackWithRetry(String commId, String eventType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("communication_id", commId);
        payload.put("event", eventType);
        payload.put("timestamp", java.time.Instant.now().toString());

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxCallbackRetries; attempt++) {
            try {
                if (attempt > 1) {
                    long backoffMs = retryBaseDelayMs * (1L << (attempt - 2)); // 100, 200, 400...
                    log.debug("Retry attempt {} for Comm {} event={} after {}ms backoff",
                            attempt, commId, eventType, backoffMs);
                    Thread.sleep(backoffMs);
                }

                ResponseEntity<Map> response = restTemplate.postForEntity(callbackUrl, payload, Map.class);
                log.debug("Callback delivered [attempt={}]: Comm={} event={} status={}",
                        attempt, commId, eventType, response.getStatusCode());
                return; // success — stop retrying

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Callback retry interrupted for Comm {} event={}", commId, eventType);
                break;
            } catch (Exception e) {
                lastException = e;
                log.warn("Callback attempt {}/{} failed for Comm {} event={}: {}",
                        attempt, maxCallbackRetries, commId, eventType, e.getMessage());
            }
        }

        // All retries exhausted — direct DB fallback to avoid losing the event
        log.error("All {} retry attempts failed for Comm {} event={}. Falling back to direct DB update.",
                maxCallbackRetries, commId, eventType);
        applyDirectDbFallback(commId, eventType);
    }

    /**
     * Last-resort fallback: updates the Communication status directly in the DB
     * when the loopback HTTP callback cannot be reached after all retries.
     */
    private void applyDirectDbFallback(String commId, String eventType) {
        try {
            communicationRepository.findById(commId).ifPresent(comm -> {
                int currentWeight  = getStatusWeight(comm.getStatus());
                int incomingWeight = getStatusWeight(eventType);
                if (incomingWeight > currentWeight) {
                    comm.setStatus(eventType);
                    comm.setUpdatedAt(java.time.LocalDateTime.now());
                    communicationRepository.save(comm);
                    log.info("DB fallback applied: Comm {} status set to {}", commId, eventType);
                }
            });
        } catch (Exception dbEx) {
            log.error("DB fallback also failed for Comm {} event={}: {}", commId, eventType, dbEx.getMessage());
        }
    }

    /** Mirrors the state machine weight in CallbackController to avoid promoting backwards. */
    private int getStatusWeight(String status) {
        if (status == null) return -1;
        switch (status.toLowerCase()) {
            case "pending":   return 0;
            case "sent":      return 1;
            case "failed":    return 2;
            case "delivered": return 3;
            case "read":      return 4;
            case "opened":    return 5;
            case "clicked":   return 6;
            case "converted": return 7;
            default:          return -1;
        }
    }
}
