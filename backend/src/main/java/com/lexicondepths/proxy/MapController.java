package com.lexicondepths.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Two endpoints. Anything more would be a feature the game does not have. */
@RestController
public class MapController {

    private static final int MAX_TOPIC_LENGTH = 40;
    private static final List<String> LEVELS = List.of("A1", "A2", "B1", "B2", "C1", "C2");

    private final DeepSeekClient client;

    public MapController(DeepSeekClient client) {
        this.client = client;
    }

    /**
     * Reports whether a key is configured, never any part of it. This is the fastest way to
     * diagnose a dead demo: if the phone can reach this, the network is fine and the key is not.
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "ok", "keyConfigured", client.hasKey());
    }

    @PostMapping("/generate-map")
    public ResponseEntity<?> generateMap(@RequestBody(required = false) Map<String, String> request) {
        String topic = request == null ? "" : String.valueOf(request.getOrDefault("topic", "")).trim();
        String level = request == null ? "" : String.valueOf(request.getOrDefault("level", ""))
                .trim().toUpperCase(Locale.ROOT);

        if (topic.isEmpty() || topic.length() > MAX_TOPIC_LENGTH) {
            return error(HttpStatus.BAD_REQUEST,
                    "Topic must be 1-" + MAX_TOPIC_LENGTH + " characters.");
        }
        if (!LEVELS.contains(level)) {
            return error(HttpStatus.BAD_REQUEST, "Level must be one of " + LEVELS + ".");
        }
        if (!client.hasKey()) {
            return error(HttpStatus.SERVICE_UNAVAILABLE,
                    "No DEEPSEEK_API_KEY set on the proxy. Set it and restart the server.");
        }

        String raw;
        try {
            raw = client.generateMap(topic.toLowerCase(Locale.ROOT), level);
        } catch (IOException e) {
            return error(HttpStatus.BAD_GATEWAY, "Could not reach the realm-forge: " + e.getMessage());
        }

        JsonNode map;
        try {
            map = MapValidator.validate(raw);
        } catch (MapValidator.InvalidMapException e) {
            return error(HttpStatus.BAD_GATEWAY, "The forge returned a broken realm: " + e.getMessage());
        }
        return ResponseEntity.ok(map);
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
