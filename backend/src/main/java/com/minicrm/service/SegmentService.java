package com.minicrm.service;

import com.minicrm.model.Customer;
import com.minicrm.model.Order;
import com.minicrm.model.Segment;
import com.minicrm.repository.CustomerRepository;
import com.minicrm.repository.OrderRepository;
import com.minicrm.repository.SegmentRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SegmentService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final SegmentRepository segmentRepository;

    /**
     * Executes the query logic on the saved segment and returns matching customers.
     */
    public List<Customer> getSegmentCustomers(String segmentId) {
        Optional<Segment> segmentOpt = segmentRepository.findById(segmentId);
        if (segmentOpt.isEmpty()) {
            return Collections.emptyList();
        }
        return evaluateFilter(segmentOpt.get().getFilterRules());
    }

    /**
     * Previews customer matching counts and rows from filter rules before saving.
     * 
     * ARCHITECTURE NOTE FOR INTERVIEW:
     * This method evaluates rules entirely in memory. It fetches all customers and all orders.
     * Trade-off: This guarantees consistency across the dual "In-Memory" and "MongoDB" data stores
     * because the logic lives in Java. However, at production scale (>1M records), this would cause
     * OutOfMemory errors. A production-ready approach would translate the `filterRules` JSON 
     * directly into MongoDB Aggregation Pipelines (or SQL WHERE clauses) to evaluate at the database level.
     */
    public List<Customer> evaluateFilter(Map<String, Object> filterRules) {
        List<Customer> allCustomers = customerRepository.findAll();
        List<Order> allOrders = orderRepository.findAll();

        // Group orders by customer ID for fast stats calculation
        Map<String, List<Order>> ordersByCustomer = allOrders.stream()
                .collect(Collectors.groupingBy(Order::getCustomerId));

        LocalDateTime now = LocalDateTime.now();
        List<Customer> matched = new ArrayList<>();

        for (Customer customer : allCustomers) {
            CustomerStats stats = calculateCustomerStats(customer.getId(), ordersByCustomer.getOrDefault(customer.getId(), Collections.emptyList()), now);
            if (evaluateRule(customer, stats, filterRules)) {
                matched.add(customer);
            }
        }

        return matched;
    }

    private CustomerStats calculateCustomerStats(String customerId, List<Order> orders, LocalDateTime now) {
        double totalSpend = 0;
        int orderCount = orders.size();
        int daysSinceLastOrder = 9999; // large default if no purchases
        Set<String> categoryAffinity = new HashSet<>();

        if (!orders.isEmpty()) {
            LocalDateTime lastOrderDate = null;
            for (Order o : orders) {
                totalSpend += o.getAmount();
                if (lastOrderDate == null || o.getOrderDate().isAfter(lastOrderDate)) {
                    lastOrderDate = o.getOrderDate();
                }
                if (o.getItems() != null) {
                    for (Order.OrderItem item : o.getItems()) {
                        if (item.getCategory() != null) {
                            categoryAffinity.add(item.getCategory());
                        }
                    }
                }
            }
            if (lastOrderDate != null) {
                daysSinceLastOrder = (int) Duration.between(lastOrderDate, now).toDays();
                // Treat negative difference (e.g. order date in the future due to timezone/seed data) as 0
                if (daysSinceLastOrder < 0) {
                    daysSinceLastOrder = 0;
                }
            }
        }

        return CustomerStats.builder()
                .totalSpend(totalSpend)
                .orderCount(orderCount)
                .daysSinceLastOrder(daysSinceLastOrder)
                .categoryAffinity(categoryAffinity)
                .build();
    }

    @SuppressWarnings("unchecked")
    private boolean evaluateRule(Customer customer, CustomerStats stats, Map<String, Object> rule) {
        if (rule == null || rule.isEmpty()) {
            return true; // Empty rule matches all
        }

        if (rule.containsKey("and")) {
            List<Object> andRules = (List<Object>) rule.get("and");
            for (Object subRule : andRules) {
                if (!evaluateRule(customer, stats, (Map<String, Object>) subRule)) {
                    return false;
                }
            }
            return true;
        }

        if (rule.containsKey("or")) {
            List<Object> orRules = (List<Object>) rule.get("or");
            for (Object subRule : orRules) {
                if (evaluateRule(customer, stats, (Map<String, Object>) subRule)) {
                    return true;
                }
            }
            return false;
        }

        // Leaf rule
        String field = (String) rule.get("field");
        String op = (String) rule.get("op");
        Object val = rule.get("value");

        if (field == null || op == null) {
            return false;
        }

        return matchLeaf(customer, stats, field, op, val);
    }

    private boolean matchLeaf(Customer customer, CustomerStats stats, String field, String op, Object val) {
        switch (field) {
            case "name":
                return compareString(customer.getName(), op, val);
            case "city":
                return compareString(customer.getCity(), op, val);
            case "tier":
                return compareString(customer.getTier(), op, val);
            case "consent":
                return compareBoolean(customer.isConsent(), op, val);
            case "days_since_last_order":
                return compareNumeric(stats.getDaysSinceLastOrder(), op, val);
            case "total_spend":
                return compareNumeric(stats.getTotalSpend(), op, val);
            case "order_count":
                return compareNumeric(stats.getOrderCount(), op, val);
            case "category_affinity":
                return evaluateCategoryAffinity(stats.getCategoryAffinity(), op, val);
            default:
                log.warn("Unknown filter field encountered: {}", field);
                return false;
        }
    }

    private boolean compareString(String customerVal, String op, Object rawVal) {
        if (customerVal == null) return false;
        String compareVal = String.valueOf(rawVal);
        switch (op) {
            case "==":
                return customerVal.equalsIgnoreCase(compareVal);
            case "!=":
                return !customerVal.equalsIgnoreCase(compareVal);
            case "contains":
                return customerVal.toLowerCase().contains(compareVal.toLowerCase());
            default:
                return false;
        }
    }

    private boolean compareBoolean(boolean customerVal, String op, Object rawVal) {
        boolean compareVal = rawVal instanceof Boolean ? (Boolean) rawVal : Boolean.parseBoolean(String.valueOf(rawVal));
        if ("==".equals(op)) {
            return customerVal == compareVal;
        } else if ("!=".equals(op)) {
            return customerVal != compareVal;
        }
        return false;
    }

    private boolean compareNumeric(double customerVal, String op, Object rawVal) {
        if (rawVal == null) return false;
        double compareVal = Double.parseDouble(String.valueOf(rawVal));
        switch (op) {
            case "==": return customerVal == compareVal;
            case "!=": return customerVal != compareVal;
            case ">": return customerVal > compareVal;
            case "<": return customerVal < compareVal;
            case ">=": return customerVal >= compareVal;
            case "<=": return customerVal <= compareVal;
            default: return false;
        }
    }

    private boolean evaluateCategoryAffinity(Set<String> categories, String op, Object rawVal) {
        if (categories == null || rawVal == null) return false;
        String val = String.valueOf(rawVal).toLowerCase();
        
        boolean contains = categories.stream().anyMatch(cat -> cat.toLowerCase().contains(val));
        if ("contains".equals(op) || "==".equals(op)) {
            return contains;
        } else if ("!=".equals(op)) {
            return !contains;
        }
        return false;
    }

    @Data
    @Builder
    private static class CustomerStats {
        private final double totalSpend;
        private final int orderCount;
        private final int daysSinceLastOrder;
        private final Set<String> categoryAffinity;
    }
}
