package com.lexicondepths.content;

import com.lexicondepths.db.CefrLevel;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Talks to the backend proxy — never to DeepSeek. The API key is not in this APK and must not be:
 * apktool would find it in a minute. See docs/phase-3.md.
 *
 * HttpURLConnection and org.json, both in the SDK. One POST does not earn a dependency.
 * Blocking; call it from App.io().
 */
public final class MapApi {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    // The proxy waits up to 60s on DeepSeek, so anything shorter here times out a live call.
    private static final int READ_TIMEOUT_MS = 70_000;

    private MapApi() {
    }

    /**
     * Returns the raw response body for a successful generation.
     *
     * @throws IOException with a player-readable message — the proxy's own {@code error} string
     *                     when it sent one, so "no key configured" reaches the screen intact.
     */
    public static String generateMap(String baseUrl, String topic, CefrLevel level)
            throws IOException {
        String url = baseUrl.replaceAll("/+$", "") + "/generate-map";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoOutput(true);

            JSONObject body = new JSONObject();
            try {
                body.put("topic", topic);
                body.put("level", level.name());
            } catch (Exception e) {
                throw new IOException("Could not build the request.");
            }
            try (OutputStream out = conn.getOutputStream()) {
                out.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                return drain(conn.getInputStream());
            }
            throw new IOException(errorMessage(drain(conn.getErrorStream()), status));
        } finally {
            conn.disconnect();
        }
    }

    /** The proxy's error bodies are written to be shown to a player; prefer them to a status code. */
    private static String errorMessage(String body, int status) {
        try {
            String message = new JSONObject(body).optString("error", "");
            if (!message.isEmpty()) {
                return message;
            }
        } catch (Exception ignored) {
            // Not JSON — a proxy that isn't ours, or a captive portal. Fall through.
        }
        return "The realm-forge answered with HTTP " + status + ".";
    }

    private static String drain(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        try (InputStream stream = in; ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[4096];
            int read;
            while ((read = stream.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toString(StandardCharsets.UTF_8.name());
        }
    }
}
