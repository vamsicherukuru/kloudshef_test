package com.kloudshef.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kloudshef.backend.entity.Cook;
import com.kloudshef.backend.repository.CookRepository;
import com.kloudshef.backend.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HuggingFaceService {

    private final ReviewRepository reviewRepository;
    private final CookRepository cookRepository;

    @Value("${huggingface.api-key:}")
    private String apiKey;

    // Classic hf-inference hosted model — free, no third-party provider needed
    private static final String HF_BASE = "https://router.huggingface.co/hf-inference/models/";
    private static final String SUMMARY_MODEL  = "google/flan-t5-large";
    private static final String GENERATE_MODEL = "google/flan-t5-large";

    // ─── Review Summary ──────────────────────────────────────────────────────────

    public String generateSummary(Long cookId) {
        try {
            Cook cook = cookRepository.findById(cookId).orElse(null);
            if (cook == null) return null;

            if (apiKey != null && !apiKey.isBlank()) {
                String inputText = buildInputText(cook, cookId);
                if (inputText != null && !inputText.isBlank()) {
                    boolean hasReviews = !reviewRepository
                            .findByCookId(cookId, Pageable.unpaged()).isEmpty();

                    String prompt = hasReviews
                            ? "Summarize these home kitchen customer reviews in 2 kind, encouraging sentences. " +
                              "Focus on what customers appreciate. If there are concerns, mention them gently " +
                              "as areas the kitchen is working on. Never use harsh or negative words: " + inputText
                            : "Write a 2-sentence warm introduction for this home kitchen: " + inputText;

                    String aiSummary = callHfInference(SUMMARY_MODEL, prompt, 100);
                    if (aiSummary != null && !aiSummary.isBlank()) {
                        cook.setReviewSummary(aiSummary);
                        cookRepository.save(cook);
                        log.info("AI summary generated for cook {}", cookId);
                        return aiSummary;
                    }
                    log.warn("AI call returned empty for cook {}, using template", cookId);
                }
            }

            String template = buildTemplateSummary(cook, cookId);
            if (template != null && !template.isBlank()) {
                cook.setReviewSummary(template);
                cookRepository.save(cook);
            }
            return template;

        } catch (Exception e) {
            log.error("generateSummary failed for cook {}: {}", cookId, e.getMessage());
            return null;
        }
    }

    @Async
    public void refreshReviewSummary(Long cookId) {
        generateSummary(cookId);
    }

    // ─── Dish Description ────────────────────────────────────────────────────────

    public String generateDishDescription(String dishName, String kitchenType, String cookingStyle) {
        if (apiKey == null || apiKey.isBlank() || dishName == null) return null;
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("Write a mouth-watering one sentence description for a home-cooked dish called ")
                  .append(dishName);
            if (cookingStyle != null && !cookingStyle.isBlank())
                prompt.append(" in ").append(cookingStyle).append(" style");
            if (kitchenType != null && !kitchenType.isBlank())
                prompt.append(" from a ").append(kitchenType).append(" kitchen");
            prompt.append(". Keep it appetizing and under 25 words.");
            return callHfInference(GENERATE_MODEL, prompt.toString(), 60);
        } catch (Exception e) {
            log.warn("generateDishDescription failed: {}", e.getMessage());
            return null;
        }
    }

    // ─── Kitchen Bio ─────────────────────────────────────────────────────────────

    public String generateKitchenBio(Long userId) {
        if (apiKey == null || apiKey.isBlank()) return null;
        try {
            Cook cook = cookRepository.findByUserId(userId).orElse(null);
            if (cook == null) return null;
            StringBuilder prompt = new StringBuilder();
            prompt.append("Write a warm and friendly 2-sentence bio for a home kitchen called ")
                  .append(cook.getKitchenName());
            if (cook.getCity() != null) prompt.append(" based in ").append(cook.getCity());
            if (cook.getCookingStyle() != null) prompt.append(" known for ").append(cook.getCookingStyle()).append(" cooking");
            if (cook.getSpecialties() != null && !cook.getSpecialties().isBlank())
                prompt.append(" specializing in ").append(cook.getSpecialties());
            prompt.append(". Sound personal and inviting.");
            return callHfInference(GENERATE_MODEL, prompt.toString(), 80);
        } catch (Exception e) {
            log.warn("generateKitchenBio failed: {}", e.getMessage());
            return null;
        }
    }

    // ─── Dish Tags ───────────────────────────────────────────────────────────────

    public List<String> generateDishTags(String dishName, String description) {
        if (apiKey == null || apiKey.isBlank() || dishName == null) return List.of();
        try {
            String prompt = "What tags apply to a dish called " + dishName +
                    (description != null && !description.isBlank() ? " described as: " + description : "") +
                    "? Choose only from: Spicy, Mild, Sweet, Sour, Vegetarian, Vegan, Non-Veg, " +
                    "Gluten-Free, Healthy, Street Food, Traditional, Quick Bite, Rich, Light. " +
                    "Reply with comma-separated tags only, nothing else.";
            String result = callHfInference(GENERATE_MODEL, prompt, 40);
            if (result == null || result.isBlank()) return List.of();
            return Arrays.stream(result.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank() && s.length() < 30)
                    .limit(4)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("generateDishTags failed: {}", e.getMessage());
            return List.of();
        }
    }

    // ─── Internal ────────────────────────────────────────────────────────────────

    private String callHfInference(String model, String prompt, int maxTokens) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15_000);
        factory.setReadTimeout(60_000);
        RestTemplate restTemplate = new RestTemplate(factory);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "inputs", prompt,
                "parameters", Map.of("max_new_tokens", maxTokens),
                "options", Map.of("wait_for_model", true)
        );

        String url = HF_BASE + model;
        log.info("HF inference [{}], prompt length: {}", model, prompt.length());
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class);
            log.info("HF response [{}]: {}", response.getStatusCode(), response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = new ObjectMapper().readTree(response.getBody());
                // hf-inference text2text response: [{"generated_text": "..."}]
                if (root.isArray() && root.size() > 0) {
                    JsonNode node = root.get(0).path("generated_text");
                    if (!node.isMissingNode() && !node.asText().isBlank()) {
                        return node.asText().trim();
                    }
                }
            }
        } catch (Exception e) {
            log.error("HF inference call failed [{}]: {}", model, e.getMessage());
        }
        return null;
    }

    private String buildInputText(Cook cook, Long cookId) {
        List<String> comments = reviewRepository
                .findByCookId(cookId, Pageable.unpaged())
                .stream()
                .map(r -> r.getComment())
                .filter(c -> c != null && !c.isBlank())
                .collect(Collectors.toList());
        String text = comments.isEmpty() ? buildProfileText(cook) : String.join(". ", comments);
        if (text != null && text.length() > 800) text = text.substring(0, 800);
        return text;
    }

    private String buildProfileText(Cook cook) {
        StringBuilder sb = new StringBuilder();
        sb.append(cook.getKitchenName()).append(" is a home kitchen");
        if (cook.getCity() != null) sb.append(" based in ").append(cook.getCity());
        if (cook.getCookingStyle() != null) sb.append(", known for ").append(cook.getCookingStyle()).append(" cooking");
        if (cook.getSpecialties() != null && !cook.getSpecialties().isBlank())
            sb.append(", specializing in ").append(cook.getSpecialties());
        if (cook.getBio() != null && !cook.getBio().isBlank())
            sb.append(". ").append(cook.getBio());
        return sb.toString();
    }

    private String buildTemplateSummary(Cook cook, Long cookId) {
        long reviewCount = reviewRepository.countByCookId(cookId);
        StringBuilder sb = new StringBuilder();

        if (reviewCount > 0) {
            double avg = cook.getAverageRating();
            sb.append(String.format("Rated %.1f★ by %d customer%s.",
                    avg, reviewCount, reviewCount == 1 ? "" : "s"));
        }
        if (cook.getSpecialties() != null && !cook.getSpecialties().isBlank()) {
            sb.append(sb.isEmpty() ? "S" : " S").append("pecializes in ")
              .append(cook.getSpecialties()).append(".");
        } else if (cook.getCookingStyle() != null && !cook.getCookingStyle().isBlank()) {
            sb.append(sb.isEmpty() ? "K" : " K").append("nown for ")
              .append(cook.getCookingStyle()).append(" cooking.");
        }
        if (cook.getCity() != null && !cook.getCity().isBlank()) {
            sb.append(sb.isEmpty() ? "A" : " A").append(" home kitchen based in ")
              .append(cook.getCity()).append(".");
        }
        if (sb.isEmpty()) {
            sb.append(cook.getKitchenName()).append(" — be the first to leave a review!");
        }
        return sb.toString();
    }
}
