package com.eagle.http.handlers;

import com.eagle.dao.BankAccountDao;
import com.eagle.dao.BankAccountDao.AccountRecord;
import com.eagle.dao.UserDao;
import com.eagle.dao.UserDao.UserRecord;
import com.eagle.model.request.CreateAccountRequest;
import com.eagle.model.response.AccountResponse;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UsersMeAccountsHandler implements HttpHandler {
    private static final Gson GSON = new Gson();

    private final UserDao userDao = new UserDao();
    private final BankAccountDao bankDao = new BankAccountDao();

    @Override
    public void handle(HttpExchange exchange) {
        try {
            String method = exchange.getRequestMethod();
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            String token = parseBearer(authHeader);
            if (token == null) {
                writeJson(exchange, 401, Map.of("ok", false, "message", "Missing or invalid Authorization header"));
                return;
            }

            UserRecord me = userDao.getUserByAuthToken(token);
            if (me == null) {
                writeJson(exchange, 401, Map.of("ok", false, "message", "Invalid token"));
                return;
            }

            // Routing: /users/me/accounts        -> collection
            //          /users/me/accounts/{id}   -> single resource
            Integer accountId = extractId(exchange.getRequestURI());

            switch (method) {
                case "POST" -> {
                    if (accountId != null) { exchange.sendResponseHeaders(405, -1); return; }
                    handleCreate(exchange, me.id);
                }
                case "GET" -> {
                    if (accountId == null) handleList(exchange, me.id);
                    else handleGetOne(exchange, me.id, accountId);
                }
                case "DELETE" -> {
                    if (accountId == null) { exchange.sendResponseHeaders(405, -1); return; }
                    handleDelete(exchange, me.id, accountId);
                }
                default -> exchange.sendResponseHeaders(405, -1);
            }
        } catch (Exception e) {
            try { writeJson(exchange, 500, Map.of("ok", false, "message", "Server error")); }
            catch (Exception ignore) {}
        }
    }

    private void handleCreate(HttpExchange exchange, int userId) throws Exception {
        CreateAccountRequest req = GSON.fromJson(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8),
                CreateAccountRequest.class
        );
        String desired = req != null && req.getAccountNumber() != null && !req.getAccountNumber().trim().isEmpty()
                ? req.getAccountNumber().trim() : null;

        AccountRecord r = bankDao.createForUser(userId, desired);
        if (r == null) {
            writeJson(exchange, 409, Map.of("ok", false, "message", "Could not create account"));
            return;
        }

        AccountResponse resp = new AccountResponse(r.id, r.accountNumber, r.balance.setScale(2, RoundingMode.DOWN).toPlainString());
        writeJson(exchange, 201, resp);
    }

    private void handleList(HttpExchange exchange, int userId) throws Exception {
        List<AccountRecord> rows = bankDao.listByUserId(userId);
        List<AccountResponse> out = new ArrayList<>();
        for (AccountRecord r : rows) {
            out.add(new AccountResponse(r.id, r.accountNumber, r.balance.setScale(2, RoundingMode.DOWN).toPlainString()));
        }
        writeJson(exchange, 200, out);
    }

    private void handleGetOne(HttpExchange exchange, int userId, int accountId) throws Exception {
        AccountRecord r = bankDao.getById(accountId);
        if (r == null || r.userId != userId) {
            writeJson(exchange, 404, Map.of("ok", false, "message", "Account not found"));
            return;
        }
        AccountResponse resp = new AccountResponse(r.id, r.accountNumber, r.balance.setScale(2, RoundingMode.DOWN).toPlainString());
        writeJson(exchange, 200, resp);
    }

    private void handleDelete(HttpExchange exchange, int userId, int accountId) throws Exception {
        AccountRecord r = bankDao.getById(accountId);
        if (r == null || r.userId != userId) {
            writeJson(exchange, 404, Map.of("ok", false, "message", "Account not found"));
            return;
        }
        if (r.balance == null || r.balance.signum() != 0) {
            writeJson(exchange, 409, Map.of(
                    "ok", false,
                    "message", "Balance must be zero before deletion",
                    "balance", r.balance == null ? "unknown" : r.balance.toPlainString()
            ));
            return;
        }

        boolean deleted = bankDao.deleteIfZeroBalance(accountId, userId);
        if (!deleted) {
            writeJson(exchange, 409, Map.of("ok", false, "message", "Could not delete account (maybe balance changed)"));
            return;
        }
        exchange.sendResponseHeaders(204, -1);
    }

    // --- helpers ---

    private static String parseBearer(String header) {
        if (header == null) return null;
        if (!header.regionMatches(true, 0, "Bearer ", 0, 7)) return null;
        String t = header.substring(7).trim();
        return t.isEmpty() ? null : t;
    }

    private static Integer extractId(URI uri) {
        // expects /users/me/accounts or /users/me/accounts/{id}
        String path = uri.getPath();
        // normalize trailing slash
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        String[] parts = path.split("/");
        // ... ["", "users", "me", "accounts"] or ["", "users", "me", "accounts", "{id}"]
        if (parts.length == 5) {
            try { return Integer.parseInt(parts[4]); } catch (NumberFormatException ignore) {}
        }
        return null;
    }

    private void writeJson(HttpExchange ex, int status, Object body) throws Exception {
        byte[] out = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, out.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(out); }
    }
}
