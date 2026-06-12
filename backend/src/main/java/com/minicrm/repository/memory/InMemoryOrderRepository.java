package com.minicrm.repository.memory;

import com.minicrm.model.Order;
import com.minicrm.repository.OrderRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(name = "crm.database.mode", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryOrderRepository implements OrderRepository {
    private final Map<String, Order> store = new ConcurrentHashMap<>();

    @Override
    public Order save(Order order) {
        if (order.getId() == null) {
            order.setId(UUID.randomUUID().toString());
        }
        store.put(order.getId(), order);
        return order;
    }

    @Override
    public List<Order> saveAll(List<Order> orders) {
        List<Order> saved = new ArrayList<>();
        for (Order o : orders) {
            saved.add(save(o));
        }
        return saved;
    }

    @Override
    public Optional<Order> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Order> findByCustomerId(String customerId) {
        if (customerId == null) return Collections.emptyList();
        return store.values().stream()
                .filter(o -> customerId.equals(o.getCustomerId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findByCustomerIdIn(List<String> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) return Collections.emptyList();
        Set<String> idSet = new HashSet<>(customerIds);
        return store.values().stream()
                .filter(o -> idSet.contains(o.getCustomerId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public void deleteAll() {
        store.clear();
    }
}
