package com.eagle.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Common HTTP helpers for all clients.
 * Subclasses can call the protected methods to keep code minimal.
 */
public abstract class AbsClient {
    protected static final String DEFAULT_BASE_URL = "http://localhost:8080";
    protected static final int DEFAULT_TIMEOUT_MS = 5000;

    // ---------- URL helpers ----------
    protected String trimTrailingSlash(String s) {
        if (s == null || s.isEmpty()) return "";
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '/') end--;
        return s.substring(0, end);
    }

    protected String joinUrl(String base, String path) {
        base = trimTrailingSlash(base);
        if (path == null || path.isEmpty()) return base;
        if (path.charAt(0) == '/') return base + path;
        return base + "/" + path;
    }

    // ---------- Connection helpers ----------
    protected HttpURLConnection open(String urlStr, String method) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setUseCaches(false);
        c.setConnectTimeout(DEFAULT_TIMEOUT_MS);
        c.setReadTimeout(DEFAULT_TIMEOUT_MS);
        c.setRequestMethod(method);
        return c;
    }

    protected void setJsonHeaders(HttpURLConnection c) {
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    }

    protected void setAuth(HttpURLConnection c, String token) {
        if (token != null && !token.isBlank()) {
            c.setRequestProperty("Authorization", "Bearer " + token.trim());
        }
    }

    protected void enableBody(HttpURLConnection c) {
        c.setDoOutput(true);
    }

    protected void writeJson(HttpURLConnection c, String json) throws Exception {
        try (OutputStream os = c.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    protected String readBody(HttpURLConnection c, int code) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                (code >= 200 && code < 300) ? c.getInputStream() : c.getErrorStream(),
                StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            for (String l; (l = br.readLine()) != null; ) sb.append(l);
            return sb.toString();
        } catch (NullPointerException npe) { // when errorStream is null
            return "";
        }
    }

    protected void setPatchOrOverride(HttpURLConnection c) throws ProtocolException {
        try {
            c.setRequestMethod("PATCH");
        } catch (ProtocolException ex) {
            c.setRequestMethod("POST");
            c.setRequestProperty("X-HTTP-Method-Override", "PATCH");
        }
    }
}