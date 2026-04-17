package com.kloudshef.backend.repository;

import com.kloudshef.backend.entity.Order;
import com.kloudshef.backend.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomer_IdOrderByCreatedAtDesc(Long customerId);
    List<Order> findByCook_IdOrderByCreatedAtDesc(Long cookId);
    long countByCook_IdAndStatus(Long cookId, OrderStatus status);
    long countByCustomer_IdAndStatusIn(Long customerId, List<OrderStatus> statuses);
}
