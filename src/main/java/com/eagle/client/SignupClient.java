package com.eagle.client;

import com.eagle.model.request.SignupRequest;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class SignupClient {

    // username -> password (kept for backwards-compat)
    public static final Map<String, String> USERS = new LinkedHashMap<>();

    // NEW: username -> bearer token (auth_token from server)
    public static final Map<String, String> TOKENS = new LinkedHashMap<>();

    private static final Random RAND = new Random();
    private static final Gson GSON = new Gson();

    // Keep track of last created (handy for quick login)
    public static volatile String LAST_USERNAME = null;
    public static volatile String LAST_PASSWORD = null;
    public static volatile String LAST_AUTH_TOKEN = null; // NEW

    /** Generate random details, signup, and store exact server username in USERS/TOKENS */
    public static String signupRandom() throws Exception {
        String username = "user" + System.currentTimeMillis() + RAND.nextInt(1000);
        String password = "pass" + RAND.nextInt(1000);
        String email = username + "@example.com";
        String dob = LocalDate.of(1990 + RAND.nextInt(20), 1 + RAND.nextInt(12), 1 + RAND.nextInt(28)).toString();
        String address = "123 Main Street";
        String pin = String.format("%06d", RAND.nextInt(999999));
        String phone = "+44" + (100000000 + RAND.nextInt(899999999));
        return signupAndStore(username, password, email, dob, address, pin, phone);
    }

    /** Call /signup with explicit creds and store EXACT username the server returns */
    public static String signupAndStore(String username, String password,
                                        String email, String dob, String address,
                                        String pin, String phone) throws Exception {
        // Build DTO
        SignupRequest req = new SignupRequest(username, password, email, dob, address, pin, phone);
        String respJson = signup(req);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = GSON.fromJson(respJson, Map.class);

            // Read fields from server response
            String serverUsername = asString(map.get("username"));
            String authToken = asString(map.get("authToken")); // <-- from updated SignupResponse

            if (serverUsername != null && !serverUsername.isBlank()) {
                // keep USERS map in sync
                USERS.remove(username);
                USERS.put(serverUsername, password);

                // NEW: store token if present
                if (authToken != null && !authToken.isBlank()) {
                    TOKENS.put(serverUsername, authToken);
                    LAST_AUTH_TOKEN = authToken;
                }

                LAST_USERNAME = serverUsername;
                LAST_PASSWORD = password;
                return respJson;
            }
        } catch (Exception ignore) {
            // fall through to legacy behavior
        }

        // Legacy fallback if parsing fails
        USERS.put(username, password);
        LAST_USERNAME = username;
        LAST_PASSWORD = password;
        // token likely missing in this path
        return respJson;
    }

    /** Overload for old 2-field version (still works if server ignores missing fields) */
    public static String signupAndStore(String username, String password) throws Exception {
        return signupAndStore(username, password, null, null, null, null, null);
    }

    /** Raw HTTP call to /signup with all fields (kept for callers) */
    public static String signup(String username, String password,
                                String email, String dob, String address,
                                String pin, String phone) throws Exception {
        return signup(new SignupRequest(username, password, email, dob, address, pin, phone));
    }

    /** NEW: Raw HTTP call that accepts a typed DTO and uses its JSON helper */
    public static String signup(SignupRequest req) throws Exception {
        URL url = new URL("http://localhost:8080/signup");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");

        // Use the same helper on the DTO
        String jsonBody = req.toJson();
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            for (String l; (l = br.readLine()) != null; ) sb.append(l);
            return sb.toString();
        }
    }

    // --- Helpers ---

    private static String asString(Object o) {
        if (o == null) return null;
        if (o instanceof String s) return s;
        return String.valueOf(o);
    }

    /** Build Authorization header value for a given username (null if missing). */
    public static String bearerFor(String username) {
        String tok = TOKENS.get(username);
        return (tok == null || tok.isBlank()) ? null : "Bearer " + tok;
    }
}
