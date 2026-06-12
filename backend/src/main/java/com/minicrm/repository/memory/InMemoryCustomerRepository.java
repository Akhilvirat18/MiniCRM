package com.minicrm.repository.memory;

import com.minicrm.model.Customer;
import com.minicrm.repository.CustomerRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "crm.database.mode", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryCustomerRepository implements CustomerRepository {
    private final Map<String, Customer> store = new ConcurrentHashMap<>();

    @Override
    public Customer save(Customer customer) {
        if (customer.getId() == null) {
            customer.setId(UUID.randomUUID().toString());
        }
        store.put(customer.getId(), customer);
        return customer;
    }

    @Override
    public List<Customer> saveAll(List<Customer> customers) {
        List<Customer> saved = new ArrayList<>();
        for (Customer c : customers) {
            saved.add(save(c));
        }
        return saved;
    }

    @Override
    public Optional<Customer> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return store.values().stream()
                .filter(c -> email.equalsIgnoreCase(c.getEmail()))
                .findFirst();
    }

    @Override
    public Optional<Customer> findByPhone(String phone) {
        if (phone == null) return Optional.empty();
        return store.values().stream()
                .filter(c -> phone.equals(c.getPhone()))
                .findFirst();
    }

    @Override
    public List<Customer> findAll() {
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
