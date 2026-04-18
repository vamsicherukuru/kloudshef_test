package com.kloudshef.backend.repository;

import com.kloudshef.backend.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByUserIdAndCookId(Long userId, Long cookId);

    List<Follow> findByUserId(Long userId);

    List<Follow> findByCookId(Long cookId);

    @Query("SELECT f.user.id FROM Follow f WHERE f.cook.id = :cookId")
    List<Long> findFollowerUserIdsByCookId(Long cookId);

    boolean existsByUserIdAndCookId(Long userId, Long cookId);

    void deleteByUserIdAndCookId(Long userId, Long cookId);

    void deleteByUserId(Long userId);

    long countByCookId(Long cookId);
}
