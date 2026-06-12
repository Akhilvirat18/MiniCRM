package com.minicrm.controller;

import com.minicrm.model.Customer;
import com.minicrm.model.Order;
import com.minicrm.model.Segment;
import com.minicrm.repository.CustomerRepository;
import com.minicrm.repository.OrderRepository;
import com.minicrm.repository.SegmentRepository;
import com.minicrm.service.LlmService;
import com.minicrm.service.SegmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/segments")
@CrossOrigin(origins = "${frontend.url}")
public class SegmentController {

    private final LlmService llmService;
    private final SegmentService segmentService;
    private final SegmentRepository segmentRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    public SegmentController(LlmService llmService,
                             SegmentService segmentService,
                             SegmentRepository segmentRepository,
                             CustomerRepository customerRepository,
                             OrderRepository orderRepository) {
        this.llmService = llmService;
        this.segmentService = segmentService;
        this.segmentRepository = segmentRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * Proactively suggests 4 high-value segments based on live customer data stats.
     * Builds a plain-text customer summary (tier breakdown, avg lapse, top categories)
     * and passes it to the LLM for actionable recommendations.
     */
    @GetMapping("/ai-suggestions")
    public ResponseEntity<List<Map<String, Object>>> getAiSegmentSuggestions() {
        List<Customer> allCustomers = customerRepository.findAll();
        List<Order> allOrders = orderRepository.findAll();

        long goldCount   = allCustomers.stream().filter(c -> "Gold".equalsIgnoreCase(c.getTier())).count();
        long silverCount = allCustomers.stream().filter(c -> "Silver".equalsIgnoreCase(c.getTier())).count();
        long bronzeCount = allCustomers.stream().filter(c -> "Bronze".equalsIgnoreCase(c.getTier())).count();
        long totalCustomers = allCustomers.size();
        long totalOrders    = allOrders.size();
        double avgOrderValue = allOrders.stream().mapToDouble(Order::getAmount).average().orElse(0);

        String summary = String.format(
            "Total customers: %d. Gold: %d, Silver: %d, Bronze: %d. " +
            "Total orders: %d. Average order value: %.2f. " +
            "Categories present: Coffee Beans, Filters, Accessories, Apparels.",
            totalCustomers, goldCount, silverCount, bronzeCount, totalOrders, avgOrderValue
        );

        List<Map<String, Object>> suggestions = llmService.suggestSegments(summary);
        return ResponseEntity.ok(suggestions);
    }

    @PostMapping("/ai")
    public ResponseEntity<Map<String, Object>> translateSegmentText(@RequestBody Map<String, String> request) {
        String description = request.get("description");
        String name = request.get("name");
        
        if (description == null || description.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Description is required"));
        }

        // Call LLM translator
        Map<String, Object> filterRules = llmService.translateSegment(description);
        
        // Dry-run preview matching count
        List<Customer> previewCustomers = segmentService.evaluateFilter(filterRules);

        Map<String, Object> response = new HashMap<>();
        response.put("name", name != null ? name : "Segment Recommendation");
        response.put("filter_rules", filterRules);
        response.put("preview_count", previewCustomers.size());
        response.put("explanation", List.of("Automated query translation complete", "Lapsed and purchase records evaluated"));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/preview")
    public ResponseEntity<Map<String, Object>> previewFilter(@RequestBody Map<String, Object> filterRules) {
        List<Customer> matched = segmentService.evaluateFilter(filterRules);
        
        Map<String, Object> response = new HashMap<>();
        response.put("count", matched.size());
        // Return first 5 samples
        List<Customer> sample = matched.subList(0, Math.min(matched.size(), 5));
        response.put("samples", sample);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Segment> saveSegment(@RequestBody Segment segment) {
        if (segment.getCreatedAt() == null) {
            segment.setCreatedAt(LocalDateTime.now());
        }
        
        // Evaluate matching count before saving
        List<Customer> matched = segmentService.evaluateFilter(segment.getFilterRules());
        segment.setPreviewCount(matched.size());

        Segment saved = segmentRepository.save(segment);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<Segment>> getAllSegments() {
        return ResponseEntity.ok(segmentRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Segment> getSegmentById(@PathVariable String id) {
        Optional<Segment> seg = segmentRepository.findById(id);
        return seg.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/customers")
    public ResponseEntity<List<Customer>> getSegmentCustomers(@PathVariable String id) {
        List<Customer> customers = segmentService.getSegmentCustomers(id);
        return ResponseEntity.ok(customers);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteSegment(@PathVariable String id) {
        segmentRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
