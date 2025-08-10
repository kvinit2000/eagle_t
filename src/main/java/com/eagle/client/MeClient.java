package com.eagle.client;

import com.eagle.model.response.UserProfileResponse;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class MeClient {
    private static final Gson GSON = new Gson();

    /** GET /users/me */
    public static UserProfileResponse getMe(String authToken) throws Exception {
        requireToken(authToken);

        URL url = new URL("http://localhost:8080/users/me");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setUseCaches(false);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + authToken);
        conn.setRequestProperty("Accept", "application/json");

        int code = conn.getResponseCode();
        String body = readBody(conn, code);
        if (code >= 200 && code < 300) {
            if (body == null || body.isBlank()) return null;
            return GSON.fromJson(body, UserProfileResponse.class);
        } else {
            throw new IOException("HTTP " + code + " GET /users/me -> " + body);
        }
    }

    /**
     * PATCH /users/me — only updates fields that are non-null/non-blank.
     * dob format: yyyy-MM-dd
     */
    public static UserProfileResponse patchMe(String authToken,
                                              String email,
                                              String dob,
                                              String address,
                                              String pin,
                                              String phone) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (!isBlank(email))   payload.put("email", email.trim());
        if (!isBlank(dob))     payload.put("dob", dob.trim());
        if (!isBlank(address)) payload.put("address", address.trim());
        if (!isBlank(pin))     payload.put("pin", pin.trim());
        if (!isBlank(phone))   payload.put("phone", phone.trim());
        return patch(authToken, payload);
    }

    /** Convenience helpers */
    public static UserProfileResponse patchEmail(String authToken, String email) throws Exception {
        return patch(authToken, Map.of("email", email));
    }
    public static UserProfileResponse patchPhone(String authToken, String phone) throws Exception {
        return patch(authToken, Map.of("phone", phone));
    }

    /** Generic PATCH using a provided map (non-null/non-blank values are sent) */
    public static UserProfileResponse patch(String authToken, Map<String, ?> fields) throws Exception {
        requireToken(authToken);

        // sanitize: drop null/blank strings
        Map<String, Object> payload = new LinkedHashMap<>();
        if (fields != null) {
            for (Map.Entry<String, ?> e : fields.entrySet()) {
                Object v = e.getValue();
                if (v == null) continue;
                if (v instanceof String s) {
                    if (isBlank(s)) continue;
                    payload.put(e.getKey(), s.trim());
                } else {
                    payload.put(e.getKey(), v);
                }
            }
        }
        if (payload.isEmpty()) {
            throw new IllegalArgumentException("No fields to update (payload would be empty).");
        }

        URL url = new URL("http://localhost:8080/users/me");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setUseCaches(false);
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        setPatch(conn); // use PATCH; fallback to POST + override if needed
        conn.setRequestProperty("Authorization", "Bearer " + authToken);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");

        String json = GSON.toJson(payload);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        String body = readBody(conn, code);
        if (code >= 200 && code < 300) {
            return (body == null || body.isBlank()) ? null : GSON.fromJson(body, UserProfileResponse.class);
        } else {
            throw new IOException("HTTP " + code + " PATCH /users/me -> " + body);
        }
    }

    // --- helpers ---

    private static void requireToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("authToken is required");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String readBody(HttpURLConnection conn, int code) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            for (String line; (line = br.readLine()) != null; ) sb.append(line);
            return sb.toString();
        } catch (NullPointerException npe) {
            // when errorStream is null (e.g., no body), just return empty
            return "";
        }
    }

    /** Try PATCH; if ProtocolException, fallback to POST + X-HTTP-Method-Override */
    private static void setPatch(HttpURLConnection conn) throws ProtocolException {
        try {
            conn.setRequestMethod("PATCH"); // JDK 21 supports this
        } catch (ProtocolException ex) {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
        }
    }
}
