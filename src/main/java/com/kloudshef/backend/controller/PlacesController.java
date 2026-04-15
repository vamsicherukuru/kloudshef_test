package com.kloudshef.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@RestController
@RequestMapping("/api/places")
public class PlacesController {

    @Value("${google.places.api-key}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @GetMapping("/autocomplete")
    public ResponseEntity<JsonNode> autocomplete(
            @RequestParam String input,
            @RequestParam(defaultValue = "address") String types) throws Exception {

        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://maps.googleapis.com/maps/api/place/autocomplete/json")
                .queryParam("input", input)
                .queryParam("types", types)
                .queryParam("language", "en")
                .queryParam("key", apiKey)
                .build()
                .toUri();

        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode body = mapper.readTree(response.body());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/reverse-geocode")
    public ResponseEntity<JsonNode> reverseGeocode(
            @RequestParam double lat,
            @RequestParam double lng) throws Exception {

        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
                .queryParam("latlng", lat + "," + lng)
                .queryParam("result_type", "locality")
                .queryParam("language", "en")
                .queryParam("key", apiKey)
                .build()
                .toUri();

        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode body = mapper.readTree(response.body());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/details/{placeId}")
    public ResponseEntity<JsonNode> details(@PathVariable String placeId) throws Exception {

        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://maps.googleapis.com/maps/api/place/details/json")
                .queryParam("place_id", placeId)
                .queryParam("fields", "formatted_address,geometry,address_components")
                .queryParam("language", "en")
                .queryParam("key", apiKey)
                .build()
                .toUri();

        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode body = mapper.readTree(response.body());
        return ResponseEntity.ok(body);
    }
}
