package com.kloudshef.backend.repository;

import com.kloudshef.backend.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByCookId(Long cookId, Pageable pageable);
    Optional<Review> findByCookIdAndUserId(Long cookId, Long userId);
    java.util.List<Review> findByUserId(Long userId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.cook.id = :cookId")
    Double getAverageRatingByCookId(@Param("cookId") Long cookId);

    long countByCookId(Long cookId);
}
