package com.kloudshef.backend.service;

import com.kloudshef.backend.entity.Cook;
import com.kloudshef.backend.entity.Subscription;
import com.kloudshef.backend.entity.SubscriptionStatus;
import com.kloudshef.backend.exception.ResourceNotFoundException;
import com.kloudshef.backend.repository.CookRepository;
import com.kloudshef.backend.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final CookRepository cookRepository;

    @Value("${app.subscription.monthly-price}")
    private BigDecimal monthlyPrice;

    @Transactional
    public Subscription activateSubscription(Long cookId, String paymentReference) {
        Cook cook = cookRepository.findById(cookId)
                .orElseThrow(() -> new ResourceNotFoundException("Cook not found"));

        LocalDate startDate = LocalDate.now();
        Subscription subscription = Subscription.builder()
                .cook(cook)
                .startDate(startDate)
                .endDate(startDate.plusMonths(1))
                .status(SubscriptionStatus.ACTIVE)
                .amountPaid(monthlyPrice)
                .paymentReference(paymentReference)
                .build();

        cook.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        cookRepository.save(cook);
        return subscriptionRepository.save(subscription);
    }

    public List<Subscription> getCookSubscriptions(Long cookId) {
        return subscriptionRepository.findByCookId(cookId);
    }

    public SubscriptionStatus getCookSubscriptionStatus(Long cookId) {
        Cook cook = cookRepository.findById(cookId)
                .orElseThrow(() -> new ResourceNotFoundException("Cook not found"));
        return cook.getSubscriptionStatus();
    }
}
