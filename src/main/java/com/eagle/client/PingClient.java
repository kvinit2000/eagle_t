package com.eagle.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class PingClient {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080";
    private static final int DEFAULT_TIMEOUT_MS = 5000;

    /** GET /hello against the default base URL. */
    public static String ping() throws Exception {
        return ping(DEFAULT_BASE_URL);
    }

    /** GET /hello against the provided base URL; returns body for 2xx or "HTTP <code> -> <body>" otherwise. */
    public static String ping(String baseUrl) throws Exception {
        String base = trimTrailingSlash(baseUrl);
        URL url = new URL(base + "/hello");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setUseCaches(false);
        conn.setConnectTimeout(DEFAULT_TIMEOUT_MS);
        conn.setReadTimeout(DEFAULT_TIMEOUT_MS);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        int code = conn.getResponseCode();
        String body = readBody(conn, code);
        return (code >= 200 && code < 300) ? body : ("HTTP " + code + (body.isBlank() ? "" : " -> " + body));
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Ping Response: " + ping());
    }

    // ===== helpers =====

    private static String readBody(HttpURLConnection c, int code) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                (code >= 200 && code < 300) ? c.getInputStream() : c.getErrorStream(),
                StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            for (String l; (l = br.readLine()) != null; ) sb.append(l);
            return sb.toString();
        } catch (NullPointerException npe) {
            // when errorStream is null
            return "";
        }
    }

    private static String trimTrailingSlash(String s) {
        if (s == null || s.isEmpty()) return "";
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '/') end--;
        return s.substring(0, end);
    }
}
