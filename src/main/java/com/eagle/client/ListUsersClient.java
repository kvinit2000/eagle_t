package com.eagle.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ListUsersClient {
    private static final String DEFAULT_BASE_URL = "http://localhost:8080";

    /** Call /listUsers on the default server; optional Bearer token not required here. */
    public static String listUsers() throws Exception {
        return listUsers(DEFAULT_BASE_URL, null);
    }

    /** Call /listUsers on the given server; Authorization header is included only when provided. */
    public static String listUsers(String baseUrl, String bearerToken) throws Exception {
        String base = trimTrailingSlash(baseUrl);
        URL url = new URL(base + "/listUsers");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        if (bearerToken != null && !bearerToken.isBlank()) {
            conn.setRequestProperty("Authorization", "Bearer " + bearerToken.trim());
        }

        int code = conn.getResponseCode();
        String body = readBody(conn, code);
        return "HTTP " + code + (body.isBlank() ? "" : " -> " + body); // JSON (UserListResponse)
    }

    public static void main(String[] args) throws Exception {
        System.out.println("User List Response: " + listUsers());
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
}
