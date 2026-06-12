package com.minicrm.repository.memory;

import com.minicrm.model.Campaign;
import com.minicrm.repository.CampaignRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "crm.database.mode", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryCampaignRepository implements CampaignRepository {
    private final Map<String, Campaign> store = new ConcurrentHashMap<>();

    @Override
    public Campaign save(Campaign campaign) {
        if (campaign.getId() == null) {
            campaign.setId(UUID.randomUUID().toString());
        }
        store.put(campaign.getId(), campaign);
        return campaign;
    }

    @Override
    public Optional<Campaign> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Campaign> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public long count() {
        return store.size();
    }
}
