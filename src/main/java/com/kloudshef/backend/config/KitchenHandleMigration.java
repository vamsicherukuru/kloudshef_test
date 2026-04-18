package com.kloudshef.backend.config;

import com.kloudshef.backend.entity.Cook;
import com.kloudshef.backend.repository.CookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class KitchenHandleMigration implements CommandLineRunner {

    private final CookRepository cookRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<Cook> cooks = cookRepository.findAll();
        int updated = 0;
        for (Cook cook : cooks) {
            if (cook.getKitchenHandle() == null || cook.getKitchenHandle().isBlank()) {
                String base = cook.getKitchenName() != null
                        ? cook.getKitchenName().toLowerCase()
                            .replaceAll("[^a-z0-9 ]", "")
                            .trim()
                            .replaceAll("\\s+", "_")
                        : "kitchen";
                if (base.isEmpty()) base = "kitchen";
                String candidate = base;
                int suffix = 1;
                while (cookRepository.existsByKitchenHandle(candidate)) {
                    candidate = base + "_" + suffix;
                    suffix++;
                }
                cook.setKitchenHandle(candidate);
                cookRepository.save(cook);
                updated++;
                log.info("Assigned handle @{} to cook #{} ({})", candidate, cook.getId(), cook.getKitchenName());
            }
        }
        if (updated > 0) {
            log.info("Kitchen handle migration: updated {} cooks", updated);
        }
    }
}
