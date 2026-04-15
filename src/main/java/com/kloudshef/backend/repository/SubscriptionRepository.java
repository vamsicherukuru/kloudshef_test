package com.kloudshef.backend.repository;

import com.kloudshef.backend.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByCookId(Long cookId);
    Optional<Subscription> findTopByCookIdOrderByEndDateDesc(Long cookId);
}
