package com.kloudshef.backend.repository;

import com.kloudshef.backend.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByCookId(Long cookId);
    List<MenuItem> findByCookIdAndAvailableTrue(Long cookId);
    void deleteByCookId(Long cookId);
}
