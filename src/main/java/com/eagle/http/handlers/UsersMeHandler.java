package com.eagle.http.handlers;

import com.eagle.dao.UserDao;
import com.eagle.dao.UserDao.UserRecord;
import com.eagle.model.response.UserProfileResponse;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class UsersMeHandler implements HttpHandler {
    private static final Logger log = LogManager.getLogger(UsersMeHandler.class);
    private static final Gson GSON = new Gson();

    private final UserDao userDao = new UserDao();

    @Override
    public void handle(HttpExchange exchange) {
        try {
            String method = exchange.getRequestMethod();
            String override = exchange.getRequestHeaders().getFirst("X-HTTP-Method-Override");
            if (override != null && !override.isBlank()) {
                method = override.trim().toUpperCase();
            }

            switch (method) {
                case "GET" -> handleGet(exchange);
                case "PATCH" -> handlePatch(exchange);
                default -> exchange.sendResponseHeaders(405, -1);
            }
        } catch (Exception e) {
            log.error("UsersMeHandler error", e);
            try { writeJson(exchange, 500, Map.of("ok", false, "message", "Server error")); }
            catch (Exception ignore) {}
        }
    }

    private void handleGet(HttpExchange exchange) throws Exception {
        String token = parseBearer(exchange.getRequestHeaders().getFirst("Authorization"));
        if (token == null) { writeJson(exchange, 401, Map.of("ok", false, "message", "Missing or invalid Authorization header")); return; }

        UserRecord u = userDao.getUserByAuthToken(token);
        if (u == null) { writeJson(exchange, 401, Map.of("ok", false, "message", "Invalid token")); return; }

        UserProfileResponse resp = new UserProfileResponse(
                u.username, u.email, (u.dob == null ? null : u.dob.toString()), u.address, u.pin, u.phone
        );
        writeJson(exchange, 200, resp);
    }

    private void handlePatch(HttpExchange exchange) throws Exception {
        String token = parseBearer(exchange.getRequestHeaders().getFirst("Authorization"));
        if (token == null) { writeJson(exchange, 401, Map.of("ok", false, "message", "Missing or invalid Authorization header")); return; }

        // Read body as Map to keep it flexible
        Map<?,?> req = GSON.fromJson(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8), Map.class);
        String email   = strOrNull(req, "email");
        String address = strOrNull(req, "address");
        String pin     = strOrNull(req, "pin");
        String phone   = strOrNull(req, "phone");
        String dobRaw  = strOrNull(req, "dob");

        LocalDate dob = null;
        if (dobRaw != null) {
            try { dob = LocalDate.parse(dobRaw); }
            catch (Exception e) { writeJson(exchange, 400, Map.of("ok", false, "message", "Invalid dob format (yyyy-MM-dd)")); return; }
        }

        if (email == null && address == null && pin == null && phone == null && dob == null) {
            writeJson(exchange, 400, Map.of("ok", false, "message", "No fields to update"));
            return;
        }

        boolean ok = userDao.patchUserByAuthToken(token, email, dob, address, pin, phone);
        if (!ok) { writeJson(exchange, 404, Map.of("ok", false, "message", "User not found or nothing changed")); return; }

        // Return updated profile
        UserRecord u = userDao.getUserByAuthToken(token);
        UserProfileResponse resp = new UserProfileResponse(
                u.username, u.email, (u.dob == null ? null : u.dob.toString()), u.address, u.pin, u.phone
        );
        writeJson(exchange, 200, resp);
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

    private void writeJson(HttpExchange ex, int status, Object body) throws Exception {
        byte[] out = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, out.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(out); }
    }
}
