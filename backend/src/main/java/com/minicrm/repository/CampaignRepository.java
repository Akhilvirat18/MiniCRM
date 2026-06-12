package com.minicrm.repository;

import com.minicrm.model.Campaign;
import java.util.List;
import java.util.Optional;

public interface CampaignRepository {
    Campaign save(Campaign campaign);
    Optional<Campaign> findById(String id);
    List<Campaign> findAll();
    long count();
}
