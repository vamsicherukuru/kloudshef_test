package com.kloudshef.backend.repository;

import com.kloudshef.backend.entity.Cook;
import com.kloudshef.backend.entity.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CookRepository extends JpaRepository<Cook, Long> {

    Optional<Cook> findByUserId(Long userId);

    Optional<Cook> findByKitchenHandle(String kitchenHandle);

    boolean existsByKitchenHandle(String kitchenHandle);

    @Query("SELECT c FROM Cook c WHERE c.subscriptionStatus IN :statuses AND LOWER(c.city) = LOWER(:city)")
    Page<Cook> findByStatusesAndCity(@Param("statuses") List<SubscriptionStatus> statuses,
                                     @Param("city") String city, Pageable pageable);

    @Query("SELECT c FROM Cook c WHERE c.subscriptionStatus IN :statuses")
    Page<Cook> findByStatuses(@Param("statuses") List<SubscriptionStatus> statuses, Pageable pageable);

    @Query("SELECT c FROM Cook c WHERE c.subscriptionStatus IN :statuses AND " +
           "(LOWER(c.city) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.kitchenName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.kitchenHandle) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.cookingStyle) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.specialties) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Cook> searchActiveCooks(@Param("statuses") List<SubscriptionStatus> statuses,
                                  @Param("query") String query,
                                  Pageable pageable);

    @Query("SELECT DISTINCT c.city FROM Cook c WHERE c.subscriptionStatus IN ('ACTIVE','TRIAL') AND c.city IS NOT NULL ORDER BY c.city")
    List<String> findDistinctActiveCities();

    @Query("SELECT c FROM Cook c WHERE c.subscriptionStatus IN :statuses")
    List<Cook> findAllByStatuses(@Param("statuses") List<SubscriptionStatus> statuses);

    @Query("SELECT c FROM Cook c WHERE c.subscriptionStatus IN :statuses AND LOWER(c.city) = LOWER(:city)")
    List<Cook> findAllByStatusesAndCity(@Param("statuses") List<SubscriptionStatus> statuses,
                                        @Param("city") String city);

    @Query("SELECT c FROM Cook c WHERE c.subscriptionStatus IN :statuses " +
           "AND c.id <> :excludeId " +
           "AND (LOWER(c.city) = LOWER(:city) OR c.cookingStyle = :cookingStyle) " +
           "ORDER BY c.averageRating DESC")
    List<Cook> findSimilarCooks(@Param("statuses") List<SubscriptionStatus> statuses,
                                @Param("excludeId") Long excludeId,
                                @Param("city") String city,
                                @Param("cookingStyle") String cookingStyle,
                                Pageable pageable);
}
