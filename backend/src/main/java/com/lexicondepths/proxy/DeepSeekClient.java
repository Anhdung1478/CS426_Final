package com.lexicondepths.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * The one place the API key is used. It is read from the environment, sent in an Authorization
 * header, and never logged, echoed, or returned in an error body.
 *
 * HttpURLConnection rather than a client library: one POST does not earn a dependency, and the
 * Android side already uses the same API for the same reason.
 */
@Component
public class DeepSeekClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    // Generating a 12-word map genuinely takes 15-30s. A default timeout fails every call.
    private static final int READ_TIMEOUT_MS = 60_000;

    private static final String SYSTEM_PROMPT = """
            You build vocabulary maps for an English learning game aimed at Vietnamese learners.

            Reply with a single JSON object and nothing else. No prose, no markdown fences.

            Schema:
            {
              "name": string, an evocative 2-4 word dungeon name for the topic,
              "topic": string, the lowercase topic slug you were given,
              "level": string, the CEFR level you were given,
              "words": array of exactly 12 objects, each:
                {
                  "headword":     string, a single lowercase English word,
                  "cefr":         string, one of A1 A2 B1 B2 C1 C2, at or within one band of the map level,
                  "pos":          string, one of noun verb adjective adverb,
                  "definition":   string, a learner-friendly definition under 15 words, never using the headword,
                  "example":      string, one natural sentence that CONTAINS the headword,
                  "viGloss":      string, the Vietnamese meaning,
                  "synonyms":     array of 0-3 single English words,
                  "antonyms":     array of 0-2 single English words,
                  "collocations": array of 0-3 short phrases that include the headword,
                  "forms":        array of 0-4 inflected or derived forms of the headword,
                  "affixKey":     string like "re-" or "-tion" if the headword has a clear affix, otherwise null,
                  "formalAlt":    string, a MORE FORMAL single-word equivalent of the headword
                                  ("kids" -> "children"), or null if the headword is already formal
                }
            }

            Hard rules:
            - Every headword must be distinct.
            - Every example sentence must contain its own headword.
            - No field may be an empty string. Use null or an empty array instead.
            - Pick words a learner at the given level would actually meet in this topic.
            """;

    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public DeepSeekClient(@Value("${deepseek.api-key:}") String apiKey,
                          @Value("${deepseek.base-url}") String baseUrl,
                          @Value("${deepseek.model}") String model) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = baseUrl;
        this.model = model;
    }

    public boolean hasKey() {
        return !apiKey.isEmpty();
    }

    /** Returns the model's raw reply — still unvalidated. MapValidator is the next stop. */
    public String generateMap(String topic, String level) throws IOException {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", model);
        body.put("temperature", 1.0);
        body.putObject("response_format").put("type", "json_object");
        var messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", SYSTEM_PROMPT);
        messages.addObject().put("role", "user")
                .put("content", "topic: " + topic + "\nlevel: " + level);

        HttpURLConnection conn = (HttpURLConnection) URI.create(baseUrl).toURL().openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoOutput(true);
            try (OutputStream out = conn.getOutputStream()) {
                out.write(MAPPER.writeValueAsBytes(body));
            }

            int status = conn.getResponseCode();
            if (status != 200) {
                // Upstream error bodies can echo request details, so only the status escapes.
                drain(conn.getErrorStream());
                throw new IOException("DeepSeek returned HTTP " + status + ".");
            }

            JsonNode envelope = MAPPER.readTree(drain(conn.getInputStream()));
            JsonNode content = envelope.at("/choices/0/message/content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new IOException("DeepSeek returned an empty reply.");
            }
            return content.asText();
        } finally {
            conn.disconnect();
        }
    }

    private static String drain(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        try (InputStream stream = in; ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            stream.transferTo(buffer);
            return buffer.toString(StandardCharsets.UTF_8);
        }
    }
}
