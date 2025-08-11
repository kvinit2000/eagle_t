package com.eagle.http.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Common utilities for lightweight HttpServer handlers.
 * - Final {@link #handle(HttpExchange)} wraps {@link #doHandle(HttpExchange)} with a 500 safety net.
 * - Helpers for JSON I/O, method checks, bearer parsing, and query params.
 */
public abstract class BaseHandler implements HttpHandler {
    protected static final Gson GSON = new Gson();

    @Override
    public final void handle(HttpExchange exchange) throws IOException {
        try {
            doHandle(exchange);
        } catch (Exception e) {
            // Last-resort safety net
            try { writeJson(exchange, 500, Map.of("ok", false, "message", "Server error")); }
            catch (Exception ignore) {
                try { exchange.sendResponseHeaders(500, -1); } catch (Exception ignored) {}
            }
        }
    }

    /** Subclasses implement their logic here. Throwing propagates to the safety net above. */
    protected abstract void doHandle(HttpExchange exchange) throws Exception;

    // ---- Method handling ----

    /** Returns true if the request method matches one of the allowed; otherwise writes 405 with Allow header. */
    protected boolean ensureMethod(HttpExchange ex, String... allowed) throws IOException {
        String m = ex.getRequestMethod();
        for (String a : allowed) if (a.equalsIgnoreCase(m)) return true;
        Headers h = ex.getResponseHeaders();
        h.set("Allow", String.join(", ", Arrays.stream(allowed).map(String::toUpperCase).toList()));
        ex.sendResponseHeaders(405, -1);
        return false;
    }

    // ---- Auth helpers ----

    /** Returns the bearer token value (without the "Bearer ") or null if missing/invalid. */
    protected String parseBearer(String header) {
        if (header == null) return null;
        if (!header.regionMatches(true, 0, "Bearer ", 0, 7)) return null;
        String t = header.substring(7).trim();
        return t.isEmpty() ? null : t;
    }

    // ---- JSON helpers ----

    protected boolean isJsonRequest(HttpExchange ex) {
        String ct = ex.getRequestHeaders().getFirst("Content-Type");
        if (ct == null) return false;
        String mime = ct.split(";", 2)[0].trim();
        return "application/json".equalsIgnoreCase(mime);
    }

    protected <T> T readJson(HttpExchange ex, Class<T> type) throws IOException, JsonSyntaxException {
        try (InputStreamReader r = new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8)) {
            return GSON.fromJson(r, type);
        }
    }

    protected void writeJson(HttpExchange ex, int status, Object body) throws IOException {
        byte[] out = (body instanceof String s) ? s.getBytes(StandardCharsets.UTF_8)
                : GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, out.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(out); }
    }

    // ---- Query helpers ----

    /** Parses the query string into a LinkedHashMap (later params override earlier ones). */
    protected Map<String, String> queryParams(HttpExchange ex) {
        String q = ex.getRequestURI().getQuery();
        Map<String,String> out = new LinkedHashMap<>();
        if (q == null || q.isEmpty()) return out;
        for (String pair : q.split("&")) {
            int i = pair.indexOf('=');
            String k = i >= 0 ? pair.substring(0, i) : pair;
            String v = i >= 0 ? pair.substring(i + 1) : "";
            out.put(k, v);
        }
        return out;
    }
}
