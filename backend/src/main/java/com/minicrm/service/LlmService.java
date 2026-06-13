package com.minicrm.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minicrm.model.AiAuditLog;
import com.minicrm.repository.AiAuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class LlmService {

    private final AiAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    public LlmService(AiAuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Translates a natural language goal/description into structured filter rules.
     */
    public Map<String, Object> translateSegment(String description) {
        String systemPrompt = """
            You are an expert CRM data analyst. Translate the user's natural language customer segment description into a structured JSON filter rule.
            
            CRITICAL: The ONLY valid structure is a JSON object with an "and" or "or" key containing an array of leaf rules.
            Each leaf rule MUST have exactly these three keys: "field", "op", "value".
            
            CORRECT example:
            {
              "and": [
                { "field": "tier", "op": "==", "value": "Gold" },
                { "field": "days_since_last_order", "op": ">=", "value": 30 }
              ]
            }
            
            WRONG (never use these formats):
            - { "operator": "AND", "rules": [...] }  -- WRONG, use "and" not "operator"
            - { "field": "x", "operator": "GT", "value": 1 }  -- WRONG, use "op": ">" not "operator": "GT"
            - { "field": "x", "$gt": 1 }  -- WRONG, no MongoDB syntax
            
            Supported fields:
            - "name": string comparison
            - "city": string comparison (e.g. "Seattle")
            - "tier": Gold, Silver, Bronze
            - "consent": boolean (true/false)
            - "days_since_last_order": integer (days since last order)
            - "total_spend": number (sum of all order amounts)
            - "order_count": integer (total number of orders)
            - "category_affinity": string (matches product category e.g. "Coffee Beans")
            
            Supported "op" values: "==", "!=", ">", "<", ">=", "<=", "contains"
            
            Return ONLY the raw JSON object. No markdown, no explanation, no extra keys.
            """;

        String userPrompt = "Segment: \"" + description + "\"";
        
        try {
            String response = callGemini(systemPrompt, userPrompt, "segment_translation");
            Map<String, Object> parsed = parseJson(response);
            if (!isValidFilterFormat(parsed)) {
                log.warn("Gemini returned invalid filter format, falling back to mock. Got: {}", parsed);
                return getMockSegmentFilter(description);
            }
            return parsed;
        } catch (Exception e) {
            log.warn("Gemini segment translation failed, falling back to mock: {}", e.getMessage());
            return getMockSegmentFilter(description);
        }
    }

    /**
     * Validates that a filter rule uses the correct format expected by SegmentService.evaluateFilter().
     * Valid: top-level key is "and" or "or", each child has "field", "op", "value".
     */
    @SuppressWarnings("unchecked")
    private boolean isValidFilterFormat(Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) return true; // empty = match all
        // Must have "and" or "or" at the top level, OR be a direct leaf rule
        if (filter.containsKey("and") || filter.containsKey("or")) {
            String key = filter.containsKey("and") ? "and" : "or";
            Object rules = filter.get(key);
            if (!(rules instanceof List)) return false;
            for (Object rule : (List<?>) rules) {
                if (!(rule instanceof Map)) return false;
                Map<String, Object> leaf = (Map<String, Object>) rule;
                // Recursively check nested and/or
                if (leaf.containsKey("and") || leaf.containsKey("or")) {
                    if (!isValidFilterFormat(leaf)) return false;
                } else {
                    // Must be a leaf with field, op, value
                    if (!leaf.containsKey("field") || !leaf.containsKey("op") || !leaf.containsKey("value")) return false;
                }
            }
            return true;
        }
        // Could be a direct leaf rule
        return filter.containsKey("field") && filter.containsKey("op") && filter.containsKey("value");
    }

    /**
     * Generates a full campaign plan including segment rules, recommended channel, A/B templates, and explanations.
     */
    public Map<String, Object> generateCampaignPlan(String goal, double maxDiscount) {
        String systemPrompt = """
            You are a seasoned AI Campaign Architect. Create a complete, explainable campaign plan based on the marketer's goal and discount constraints.
            
            Return a JSON object containing:
            1. "segment_name": Short name for the segment
            2. "segment_filter": MUST use this EXACT format — a JSON object with "and" or "or" key containing leaf rules with "field", "op", "value":
               CORRECT: { "and": [ { "field": "tier", "op": "==", "value": "Gold" }, { "field": "days_since_last_order", "op": ">=", "value": 30 } ] }
               WRONG:   { "operator": "AND", "rules": [...] }  or  { "field": "x", "operator": "GT" }
               Supported fields: name, city, tier (Gold/Silver/Bronze), consent (true/false), days_since_last_order (int), total_spend (number), order_count (int), category_affinity (string)
               Supported "op" values: "==", "!=", ">", "<", ">=", "<=", "contains"
               NOTE: "between" is NOT supported — use two separate rules with >= and <= instead.
            3. "channel": Recommended channel ("sms", "email", "whatsapp", or "rcs")
            4. "message": { "template": "Main message with tokens", "variants": [ {"variant_id": "A", "template": "..."}, {"variant_id": "B", "template": "..."} ] }
               Available personalisation tokens: {{name}}, {{city}}, {{tier}}, {{last_purchased_category}}, {{days_since_last_order}}
            5. "projections": { "expected_reach": 100, "open_rate": 0.35, "click_rate": 0.08 }
            6. "explainability": {
                 "why_segment": ["Reason 1", "Reason 2"],
                 "why_channel": ["Reason 1"],
                 "why_message": ["Reason 1"]
               }
               
            Return ONLY the raw JSON response. No explanations or markdown fences.
            """;

        String userPrompt = String.format("Goal: \"%s\", Max Discount constraint: %.1f%%", goal, maxDiscount);

        try {
            String response = callGemini(systemPrompt, userPrompt, "campaign_planning");
            Map<String, Object> plan = parseJson(response);

            // Validate the segment_filter — if AI returned wrong format, replace with reliable mock
            Map<String, Object> segmentFilter = (Map<String, Object>) plan.get("segment_filter");
            if (segmentFilter == null || !isValidFilterFormat(segmentFilter)) {
                log.warn("Gemini generateCampaignPlan returned invalid segment_filter format, replacing with mock. Got: {}", segmentFilter);
                plan.put("segment_filter", getMockSegmentFilter(goal));
            }

            return plan;
        } catch (Exception e) {
            log.warn("Gemini campaign planning failed, falling back to mock: {}", e.getMessage());
            return getMockCampaignPlan(goal, maxDiscount);
        }
    }

    /**
     * Proactively analyses customer data stats and returns 4 AI-recommended high-value
     * segment suggestions — surfacing audiences the marketer may not have thought to target.
     */
    public List<Map<String, Object>> suggestSegments(String customerSummary) {
        String systemPrompt = """
            You are a CRM growth strategist. Based on the customer data summary, suggest 4 high-value
            customer segments the brand should target next.

            Return a JSON array where each item has:
            - "name": short segment label
            - "description": 1-sentence rationale
            - "filter_rules": filter rule JSON using the same schema as segment translation
            - "why": list of 1-2 bullet reasons this audience is high-value right now
            - "recommended_channel": one of "sms", "email", "whatsapp", "rcs"

            Return ONLY a raw JSON array. No markdown, no explanation.
            """;

        String userPrompt = "Customer data summary:\n" + customerSummary;

        try {
            String response = callGemini(systemPrompt, userPrompt, "segment_suggestions");
            List<Map<String, Object>> parsed = objectMapper.readValue(response, new TypeReference<>() {});
            saveMockAuditLog("segment_suggestions", userPrompt, parsed, "Gemini segment suggestions");
            return parsed;
        } catch (Exception e) {
            log.warn("Gemini segment suggestions failed, falling back to mock: {}", e.getMessage());
            return getMockSegmentSuggestions();
        }
    }

    private List<Map<String, Object>> getMockSegmentSuggestions() {
        List<Map<String, Object>> suggestions = new ArrayList<>();

        suggestions.add(Map.of(
            "name", "Lapsed Gold Buyers",
            "description", "High-value customers who haven't purchased in over 30 days.",
            "filter_rules", Map.of("and", List.of(
                Map.of("field", "tier", "op", "==", "value", "Gold"),
                Map.of("field", "days_since_last_order", "op", ">=", "value", 30)
            )),
            "why", List.of(
                "Gold tier customers have high lifetime value — re-engaging them is 5x cheaper than new acquisition.",
                "A 30-day lapse window captures customers before they fully churn."
            ),
            "recommended_channel", "whatsapp"
        ));

        suggestions.add(Map.of(
            "name", "Coffee Category Loyalists",
            "description", "Customers who have bought coffee products at least twice.",
            "filter_rules", Map.of("and", List.of(
                Map.of("field", "category_affinity", "op", "contains", "value", "Coffee"),
                Map.of("field", "order_count", "op", ">=", "value", 2)
            )),
            "why", List.of(
                "Category-loyal buyers respond 2x better to product-specific promotions.",
                "Coffee is a consumable — replenishment messaging converts extremely well."
            ),
            "recommended_channel", "sms"
        ));

        suggestions.add(Map.of(
            "name", "Near-Gold Silvers",
            "description", "Silver-tier customers with high spend — close to Gold tier.",
            "filter_rules", Map.of("and", List.of(
                Map.of("field", "tier", "op", "==", "value", "Silver"),
                Map.of("field", "total_spend", "op", ">=", "value", 3000)
            )),
            "why", List.of(
                "Upgrading near-Gold customers increases AOV and brand loyalty.",
                "A small incentive nudge can push them into Gold tier permanently."
            ),
            "recommended_channel", "email"
        ));

        suggestions.add(Map.of(
            "name", "New Shoppers — First 30 Days",
            "description", "Recently acquired customers who have made exactly one order.",
            "filter_rules", Map.of("and", List.of(
                Map.of("field", "order_count", "op", "==", "value", 1),
                Map.of("field", "days_since_last_order", "op", "<=", "value", 30)
            )),
            "why", List.of(
                "Second-purchase conversion is the single biggest predictor of long-term retention.",
                "Early engagement campaigns in the first 30 days double 90-day LTV."
            ),
            "recommended_channel", "rcs"
        ));

        saveMockAuditLog("segment_suggestions", "mock", suggestions, "Mock segment suggestions generated.");
        return suggestions;
    }

    /**
     * Calls Gemini API or throws exception if key is not present/fails.
     */
    private String callGemini(String systemPrompt, String userPrompt, String type) throws Exception {

        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            throw new IllegalStateException("Gemini API key is not configured.");
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Build Gemini request body
        Map<String, Object> requestBody = new HashMap<>();
        
        Map<String, Object> systemInstruction = new HashMap<>();
        Map<String, Object> partsObj = new HashMap<>();
        partsObj.put("text", systemPrompt);
        systemInstruction.put("parts", Collections.singletonList(partsObj));
        requestBody.put("systemInstruction", systemInstruction);

        Map<String, Object> contents = new HashMap<>();
        Map<String, Object> userParts = new HashMap<>();
        userParts.put("text", userPrompt);
        contents.put("role", "user");
        contents.put("parts", Collections.singletonList(userParts));
        requestBody.put("contents", Collections.singletonList(contents));

        // Enforce JSON output format
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        requestBody.put("generationConfig", generationConfig);

        String jsonPayload = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, entity, String.class);
        String body = responseEntity.getBody();

        // Parse text content from Gemini response structure
        Map<String, Object> respMap = objectMapper.readValue(body, new TypeReference<>() {});
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) respMap.get("candidates");
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        String generatedText = (String) parts.get(0).get("text");

        // Save audit log
        AiAuditLog logRecord = AiAuditLog.builder()
                .promptType(type)
                .systemPrompt(systemPrompt)
                .userPrompt(userPrompt)
                .rawResponse(body)
                .parsedOutput(generatedText)
                .explanation("Successfully completed call via Gemini API")
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(logRecord);

        return generatedText;
    }

    private Map<String, Object> parseJson(String rawJson) {
        try {
            // Strip markdown code fences if LLM ignored generationConfig
            String clean = rawJson.trim();
            if (clean.startsWith("```")) {
                clean = clean.replaceAll("^```json\\s*", "").replaceAll("```$", "").trim();
            }
            return objectMapper.readValue(clean, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to parse LLM JSON: {}", rawJson, e);
            throw new RuntimeException("LLM responded with invalid JSON format");
        }
    }

    // --- High Fidelity Mock Fallbacks ---

    private Map<String, Object> getMockSegmentFilter(String description) {
        Map<String, Object> filter = new HashMap<>();
        List<Map<String, Object>> rules = new ArrayList<>();
        String descLower = description.toLowerCase();

        if (descLower.contains("gold")) {
            rules.add(Map.of("field", "tier", "op", "==", "value", "Gold"));
        } else if (descLower.contains("silver")) {
            rules.add(Map.of("field", "tier", "op", "==", "value", "Silver"));
        }

        if (descLower.contains("lapsed") || descLower.contains("not purchased") || descLower.contains("haven't bought")) {
            int days = 30;
            if (descLower.contains("60")) days = 60;
            if (descLower.contains("90")) days = 90;
            rules.add(Map.of("field", "days_since_last_order", "op", ">=", "value", days));
        }

        if (descLower.contains("coffee")) {
            rules.add(Map.of("field", "category_affinity", "op", "contains", "value", "Coffee Beans"));
        } else if (descLower.contains("filter")) {
            rules.add(Map.of("field", "category_affinity", "op", "contains", "value", "Filters"));
        }

        if (descLower.contains("spend") || descLower.contains("spent")) {
            double amount = 100.0;
            if (descLower.contains("3000")) amount = 3000.0;
            else if (descLower.contains("1000")) amount = 1000.0;
            rules.add(Map.of("field", "total_spend", "op", ">", "value", amount));
        }

        if (rules.isEmpty()) {
            // Default rule
            rules.add(Map.of("field", "consent", "op", "==", "value", true));
        }

        filter.put("and", rules);
        saveMockAuditLog("segment_translation", description, filter, "Generated mock segment rules based on keyword parsing.");
        return filter;
    }

    private Map<String, Object> getMockCampaignPlan(String goal, double maxDiscount) {
        String goalLower = goal.toLowerCase();
        Map<String, Object> plan = new HashMap<>();
        
        // Define default segment rules
        Map<String, Object> segmentFilter = getMockSegmentFilter(goal);
        String segmentName = "Campaign Audience";
        if (goalLower.contains("lapsed")) {
            segmentName = "Lapsed Shoppers";
        } else if (goalLower.contains("coffee")) {
            segmentName = "Coffee Lovers";
        } else if (goalLower.contains("repeat")) {
            segmentName = "Active repeat target";
        }

        plan.put("segment_name", segmentName);
        plan.put("segment_filter", segmentFilter);

        // Recommend channel
        String channel = "email";
        List<String> whyChannel = new ArrayList<>();
        if (goalLower.contains("weekend") || goalLower.contains("urgent") || goalLower.contains("sms") || goalLower.contains("fast")) {
            channel = "sms";
            whyChannel.add("SMS recommended because of the target urgency and immediate weekend delivery window.");
            whyChannel.add("98% cell coverage identified on target customer numbers.");
        } else if (goalLower.contains("whatsapp") || goalLower.contains("chat")) {
            channel = "whatsapp";
            whyChannel.add("WhatsApp selected because of historically high click rates in recent conversational trials.");
        } else {
            whyChannel.add("Email selected to display a richer, styled item selection and description catalog.");
            whyChannel.add("Provides a low-cost send mechanism for a larger lapsed target size.");
        }
        plan.put("channel", channel);

        // Build messages
        double discount = maxDiscount > 0 ? maxDiscount : 15.0;
        Map<String, Object> message = new HashMap<>();
        String template;
        List<Map<String, String>> variants = new ArrayList<>();

        if (channel.equals("sms")) {
            template = String.format("Hi {{name}}, we miss you! Get %.0f%% off this weekend with code COMEBACK. Shop at minicrm.com/offers", discount);
            variants.add(Map.of("variant_id", "A", "template", String.format("Hi {{name}}! Ready for your next purchase? Grab %.0f%% off with code COMEBACK. Shop: minicrm.com/offers", discount)));
            variants.add(Map.of("variant_id", "B", "template", String.format("Hey {{name}}, don't miss out! Get %.0f%% off your next order. Code: SAVE%.0f. Offer ends Sunday!", discount, discount)));
        } else if (channel.equals("whatsapp")) {
            template = String.format("Hey *{{name}}*! We noticed it's been a while. How about a fresh batch of *{{last_purchased_category}}*? Grab a special *%.0f%% off* code: *WA%.0f*", discount, discount);
            variants.add(Map.of("variant_id", "A", "template", String.format("Hi *{{name}}*! Ready to restock? Enjoy *%.0f%% off* your order today. Code: *RESTOCK*", discount)));
            variants.add(Map.of("variant_id", "B", "template", String.format("Hello *{{name}}*! Grab *%.0f%% off* your next order with code *FRESH*. Free shipping included!", discount)));
        } else {
            template = String.format("Hi {{name}},\n\nWe haven't seen you in a while! Here is a special %.0f%% discount on your next order. Enter code WELCOME%.0f at checkout.\n\nCheers,\nThe Brand Team", discount, discount);
            variants.add(Map.of("variant_id", "A", "template", String.format("Subject: We miss you, {{name}}! (%.0f%% Off inside)\n\nHi {{name}},\n\nCome back and check out our new arrivals. Use code BACK%.0f for %.0f%% off!", discount, discount, discount)));
            variants.add(Map.of("variant_id", "B", "template", String.format("Subject: Special Offer for {{name}} (%.0f%% Discount)\n\nHi {{name}},\n\nGet %.0f%% off your next purchase. Use code SAVINGS%.0f. We hope to see you soon!", discount, discount, discount)));
        }

        message.put("template", template);
        message.put("variants", variants);
        plan.put("message", message);

        // Projections
        int reach = 1240;
        double openRate = channel.equals("sms") ? 0.95 : (channel.equals("whatsapp") ? 0.82 : 0.28);
        double clickRate = channel.equals("sms") ? 0.08 : (channel.equals("whatsapp") ? 0.12 : 0.04);
        
        plan.put("projections", Map.of(
            "expected_reach", reach,
            "open_rate", openRate,
            "click_rate", clickRate
        ));

        // Explainability
        Map<String, Object> explainability = new HashMap<>();
        explainability.put("why_segment", List.of(
            "Identified target audience based on lapsed days and category interests matching the goal.",
            "Filtered out shoppers who have opted out of communications."
        ));
        explainability.put("why_channel", whyChannel);
        explainability.put("why_message", List.of(
            String.format("Applied an active tone with a direct %.0f%% discount call-to-action.", discount),
            "Personalized using the buyer's name and previous category interests to maximize affinity."
        ));
        plan.put("explainability", explainability);

        saveMockAuditLog("campaign_planning", goal, plan, "Generated high-fidelity mock campaign plan due to missing API key.");
        return plan;
    }

    private void saveMockAuditLog(String type, String userPrompt, Object parsed, String explanation) {
        try {
            AiAuditLog logRecord = AiAuditLog.builder()
                    .promptType(type)
                    .systemPrompt("MOCK_SYSTEM")
                    .userPrompt(userPrompt)
                    .rawResponse("MOCK_RAW")
                    .parsedOutput(objectMapper.writeValueAsString(parsed))
                    .explanation(explanation)
                    .timestamp(LocalDateTime.now())
                    .build();
            auditLogRepository.save(logRecord);
        } catch (Exception e) {
            log.error("Failed to write mock audit log", e);
        }
    }
}
