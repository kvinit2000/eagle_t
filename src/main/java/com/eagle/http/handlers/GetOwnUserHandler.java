package com.eagle.http.handlers;

import com.eagle.dao.UserDao;
import com.eagle.dao.UserDao.UserRecord;
import com.eagle.model.response.UserProfileResponse;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class GetOwnUserHandler implements HttpHandler {
    private static final Gson GSON = new Gson();
    private final UserDao userDao = new UserDao();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        // Expect: Authorization: Bearer <token>
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        String token = parseBearer(authHeader);
        if (token == null) {
            sendError(exchange, 401, "Missing or invalid Authorization header");
            return;
        }

        // Look up user by token
        UserRecord u = userDao.getUserByAuthToken(token);
        if (u == null) {
            sendError(exchange, 401, "Invalid token");
            return;
        }

        String dobOut = (u.dob == null) ? null : u.dob.toString(); // yyyy-MM-dd
        UserProfileResponse resp = new UserProfileResponse(
                u.username,
                u.email,
                dobOut,
                u.address,
                u.pin,
                u.phone
        );

        sendJson(exchange, 200, resp);
    }

    private static String parseBearer(String header) {
        if (header == null) return null;
        if (!header.regionMatches(true, 0, "Bearer ", 0, 7)) return null;
        String t = header.substring(7).trim();
        return t.isEmpty() ? null : t;
    }

    private void sendError(HttpExchange ex, int status, String msg) throws IOException {
        sendJson(ex, status, Map.of("error", msg));
    }

    private void sendJson(HttpExchange ex, int status, Object body) throws IOException {
        byte[] out = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, out.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(out); }
    }
}
