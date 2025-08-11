package com.eagle.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LoginClient {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080";

    /** Call /login with given credentials against the default base URL and return raw JSON (LoginResponse). */
    public static String login(String username, String password) throws Exception {
        return login(DEFAULT_BASE_URL, username, password);
    }

    /** Call /login on the provided baseUrl and return raw JSON (LoginResponse). */
    public static String login(String baseUrl, String username, String password) throws Exception {
        String base = trimTrailingSlash(baseUrl);
        URL url = new URL(base + "/login");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");

        String jsonBody = "{\"username\":\"" + escape(username) + "\",\"password\":\"" + escape(password) + "\"}";
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        // Preserve original behavior: return the raw JSON body from either input or error stream
        return readBody(conn, code);
    }

    /** Standalone run for manual testing. */
    public static void main(String[] args) throws Exception {
        System.out.println("Login Response: " + login("testUser", "pass123"));
    }

    // ===== helpers =====

    private static String readBody(HttpURLConnection c, int code) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                (code >= 200 && code < 300) ? c.getInputStream() : c.getErrorStream(),
                StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            for (String line; (line = br.readLine()) != null; ) sb.append(line);
            return sb.toString();
        }
    }

    private static String trimTrailingSlash(String s) {
        if (s == null || s.isEmpty()) return "";
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '/') end--;
        return s.substring(0, end);
    }

    private static String escape(String s) {
        if (s == null) return "";
        // minimal JSON string escape for quotes/backslashes
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
