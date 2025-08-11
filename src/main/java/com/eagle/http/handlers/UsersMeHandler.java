package com.eagle.http.handlers;

import com.eagle.dao.UserDao;
import com.eagle.dao.UserDao.UserRecord;
import com.eagle.model.response.UserProfileResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

public class UsersMeHandler implements HttpHandler {
    private static final Logger log = LogManager.getLogger(UsersMeHandler.class);
    private static final Gson GSON = new Gson();

    private final UserDao userDao = new UserDao();

    @Override
    public void handle(HttpExchange exchange) {
        try {
            String method = effectiveMethod(exchange);

            switch (method) {
                case "GET" -> handleGet(exchange);
                case "PATCH" -> handlePatch(exchange);
                case "OPTIONS" -> methodNotAllowed(exchange, "GET, PATCH, OPTIONS");
                default -> methodNotAllowed(exchange, "GET, PATCH, OPTIONS");
            }
        } catch (Exception e) {
            log.error("UsersMeHandler error", e);
            try { sendJson(exchange, 500, Map.of("ok", false, "message", "Server error")); }
            catch (Exception ignore) {}
        }
    }

    private void handleGet(HttpExchange exchange) throws Exception {
        String token = getAuthToken(exchange);
        if (token == null) return;

        UserRecord u = userDao.getUserByAuthToken(token);
        if (u == null) { sendError(exchange, 401, "Invalid token"); return; }

        UserProfileResponse resp = new UserProfileResponse(
                u.username,
                u.email,
                (u.dob == null ? null : u.dob.toString()),
                u.address,
                u.pin,
                u.phone
        );
        sendJson(exchange, 200, resp);
    }

    private void handlePatch(HttpExchange exchange) throws Exception {
        if (!isJson(exchange)) {
            sendError(exchange, 415, "Content-Type must be application/json");
            return;
        }

        String token = getAuthToken(exchange);
        if (token == null) return;

        UserRecord authed = userDao.getUserByAuthToken(token);
        if (authed == null) { sendError(exchange, 401, "Invalid token"); return; }

        Map<?, ?> req;
        try {
            req = fromJson(exchange, Map.class);
        } catch (JsonSyntaxException jse) {
            sendError(exchange, 400, "Malformed JSON body");
            return;
        }

        String email   = strOrNull(req, "email");
        String address = strOrNull(req, "address");
        String pin     = strOrNull(req, "pin");
        String phone   = strOrNull(req, "phone");
        String dobRaw  = strOrNull(req, "dob");

        LocalDate dob = null;
        if (dobRaw != null) {
            try { dob = LocalDate.parse(dobRaw); }
            catch (Exception e) { sendError(exchange, 400, "Invalid dob format (yyyy-MM-dd)"); return; }
        }

        if (email == null && address == null && pin == null && phone == null && dob == null) {
            sendError(exchange, 400, "No fields to update");
            return;
        }

        boolean ok = userDao.patchUserByAuthToken(token, email, dob, address, pin, phone);
        if (!ok) {
            sendError(exchange, 404, "User not found or nothing changed");
            return;
        }

        UserRecord u = userDao.getUserByAuthToken(token);
        UserProfileResponse resp = new UserProfileResponse(
                u.username, u.email, (u.dob == null ? null : u.dob.toString()), u.address, u.pin, u.phone
        );
        sendJson(exchange, 200, resp);
    }

    private String getAuthToken(HttpExchange exchange) throws IOException {
        String token = parseBearer(exchange.getRequestHeaders().getFirst("Authorization"));
        if (token == null) { sendError(exchange, 401, "Missing or invalid Authorization header"); return null; }
        return token;
    }

    private static String effectiveMethod(HttpExchange exchange) {
        String method = exchange.getRequestMethod();
        String override = exchange.getRequestHeaders().getFirst("X-HTTP-Method-Override");
        if (override != null && !override.isBlank()) {
            method = override.trim().toUpperCase();
        }
        return method;
    }

    private static String strOrNull(Map<?,?> m, String key) {
        if (m == null) return null;
        Object v = m.get(key);
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private static String parseBearer(String header) {
        if (header == null) return null;
        if (!header.regionMatches(true, 0, "Bearer ", 0, 7)) return null;
        String t = header.substring(7).trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean isJson(HttpExchange ex) {
        final String ct = ex.getRequestHeaders().getFirst("Content-Type");
        if (ct == null) return false;
        final String mime = ct.split(";", 2)[0].trim();
        return "application/json".equalsIgnoreCase(mime);
    }

    private static <T> T fromJson(HttpExchange exchange, Class<T> clazz) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, clazz);
        }
    }

    private static void sendJson(HttpExchange ex, int status, Object body) throws IOException {
        byte[] out = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        Headers h = ex.getResponseHeaders();
        h.set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, out.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(out); }
    }

    private static void sendError(HttpExchange ex, int status, String message) throws IOException {
        sendJson(ex, status, Map.of("ok", false, "message", message));
    }

    private static void methodNotAllowed(HttpExchange ex, String allowHeader) throws IOException {
        ex.getResponseHeaders().set("Allow", allowHeader);
        ex.sendResponseHeaders(405, -1);
    }
}
