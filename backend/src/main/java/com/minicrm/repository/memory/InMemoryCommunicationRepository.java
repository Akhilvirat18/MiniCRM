package com.minicrm.repository.memory;

import com.minicrm.model.Communication;
import com.minicrm.repository.CommunicationRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(name = "crm.database.mode", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryCommunicationRepository implements CommunicationRepository {
    private final Map<String, Communication> store = new ConcurrentHashMap<>();

    @Override
    public Communication save(Communication communication) {
        if (communication.getId() == null) {
            communication.setId(UUID.randomUUID().toString());
        }
        store.put(communication.getId(), communication);
        return communication;
    }

    @Override
    public List<Communication> saveAll(List<Communication> communications) {
        List<Communication> saved = new ArrayList<>();
        for (Communication c : communications) {
            saved.add(save(c));
        }
        return saved;
    }

    @Override
    public Optional<Communication> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Communication> findByCampaignId(String campaignId) {
        if (campaignId == null) return Collections.emptyList();
        return store.values().stream()
                .filter(c -> campaignId.equals(c.getCampaignId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Communication> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public long count() {
        return store.size();
    }
}
