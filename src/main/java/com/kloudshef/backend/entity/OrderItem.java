package com.kloudshef.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    private Long menuItemId;

    private String itemName;

    @Column(precision = 10, scale = 2)
    private BigDecimal itemPrice;

    private Integer quantity;

    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;

    private boolean isVegetarian;
}
