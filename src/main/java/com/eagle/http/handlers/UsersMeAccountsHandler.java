package com.eagle.http.handlers;

import com.eagle.dao.BankAccountDao;
import com.eagle.dao.BankAccountDao.AccountRecord;
import com.eagle.dao.TransactionDao;
import com.eagle.dao.TransactionDao.TxRecord;
import com.eagle.dao.UserDao;
import com.eagle.dao.UserDao.UserRecord;
import com.eagle.model.request.CreateAccountRequest;
import com.eagle.model.request.TransactionRequest;
import com.eagle.model.response.AccountResponse;
import com.eagle.model.response.TransactionResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Refactored handler for /users/me/accounts and nested /transactions.
 * Improvements:
 * - Consistent JSON error responses via sendError()
 * - Validates Content-Type for JSON bodies
 * - Adds pagination to transaction list: ?limit=..&offset=..
 * - Sets Location header on 201 creates
 * - Returns Allow header on 405 Method Not Allowed
 * - Safer route parsing and header handling
 */
public class UsersMeAccountsHandler implements HttpHandler {
    private static final Gson GSON = new Gson();
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    private final UserDao userDao = new UserDao();
    private final BankAccountDao bankDao = new BankAccountDao();
    private final TransactionDao txDao = new TransactionDao();

    @Override
    public void handle(HttpExchange exchange) {
        try {
            final String method = exchange.getRequestMethod();

            // ---- Auth: Authorization: Bearer <token>
            final String token = parseBearer(exchange.getRequestHeaders().getFirst("Authorization"));
            if (token == null) {
                sendError(exchange, 401, "Missing or invalid Authorization header");
                return;
            }
            final UserRecord me = userDao.getUserByAuthToken(token);
            if (me == null) {
                sendError(exchange, 401, "Invalid token");
                return;
            }

            // ---- Route parsing
            final RouteInfo route = parseRoute(exchange.getRequestURI()); // accountId and /transactions
            final Integer accountId = route.accountId;
            final boolean isTx = route.isTransactions;

            switch (method) {
                case "POST" -> {
                    if (!isJson(exchange)) {
                        sendError(exchange, 415, "Content-Type must be application/json");
                        return;
                    }
                    if (isTx && accountId != null) {
                        handleTxCreate(exchange, me.id, accountId);
                    } else if (!isTx && accountId == null) {
                        handleCreate(exchange, me.id);
                    } else {
                        methodNotAllowed(exchange, allowedFor(route));
                    }
                }
                case "GET" -> {
                    if (isTx && accountId != null) {
                        handleTxList(exchange, me.id, accountId);
                    } else if (!isTx && accountId == null) {
                        handleList(exchange, me.id);
                    } else if (!isTx && accountId != null) {
                        handleGetOne(exchange, me.id, accountId);
                    } else {
                        methodNotAllowed(exchange, allowedFor(route));
                    }
                }
                case "DELETE" -> {
                    if (!isTx && accountId != null) {
                        handleDelete(exchange, me.id, accountId);
                    } else {
                        methodNotAllowed(exchange, allowedFor(route));
                    }
                }
                case "OPTIONS" -> // basic CORS / method discovery
                        methodNotAllowed(exchange, allowedFor(route));
                default -> methodNotAllowed(exchange, allowedFor(route));
            }
        } catch (Exception e) {
            try { sendJson(exchange, 500, Map.of("ok", false, "message", "Server error")); }
            catch (Exception ignore) {}
        }
    }

    // ========= Accounts =========

    private void handleCreate(HttpExchange exchange, int userId) throws Exception {
        final CreateAccountRequest req = fromJson(exchange, CreateAccountRequest.class);
        final String desired = (req != null && req.getAccountNumber() != null && !req.getAccountNumber().trim().isEmpty())
                ? req.getAccountNumber().trim()
                : null;

        final AccountRecord r = bankDao.createForUser(userId, desired);
        if (r == null) {
            sendError(exchange, 409, "Could not create account");
            return;
        }

        final AccountResponse resp = new AccountResponse(
                r.id,
                r.accountNumber,
                (r.balance == null ? "0.00" : r.balance.setScale(2, RoundingMode.DOWN).toPlainString())
        );
        setLocation(exchange, "/users/me/accounts/" + r.id);
        sendJson(exchange, 201, resp);
    }

    private void handleList(HttpExchange exchange, int userId) throws Exception {
        final List<AccountRecord> rows = bankDao.listByUserId(userId);
        final List<AccountResponse> out = new ArrayList<>();
        for (AccountRecord r : rows) {
            out.add(new AccountResponse(
                    r.id,
                    r.accountNumber,
                    (r.balance == null ? "0.00" : r.balance.setScale(2, RoundingMode.DOWN).toPlainString())
            ));
        }
        sendJson(exchange, 200, out);
    }

    private void handleGetOne(HttpExchange exchange, int userId, int accountId) throws Exception {
        final AccountRecord r = bankDao.getById(accountId);
        if (r == null || r.userId != userId) {
            sendError(exchange, 404, "Account not found");
            return;
        }
        final AccountResponse resp = new AccountResponse(
                r.id,
                r.accountNumber,
                (r.balance == null ? "0.00" : r.balance.setScale(2, RoundingMode.DOWN).toPlainString())
        );
        sendJson(exchange, 200, resp);
    }

    private void handleDelete(HttpExchange exchange, int userId, int accountId) throws Exception {
        final AccountRecord r = bankDao.getById(accountId);
        if (r == null || r.userId != userId) {
            sendError(exchange, 404, "Account not found");
            return;
        }
        if (r.balance == null || r.balance.signum() != 0) {
            sendJson(exchange, 409, Map.of(
                    "ok", false,
                    "message", "Balance must be zero before deletion",
                    "balance", r.balance == null ? "unknown" : r.balance.toPlainString()
            ));
            return;
        }

        final boolean deleted = bankDao.deleteIfZeroBalance(accountId, userId);
        if (!deleted) {
            sendError(exchange, 409, "Could not delete account (maybe balance changed)");
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(204, -1);
    }

    // ========= Transactions =========

    private void handleTxCreate(HttpExchange exchange, int userId, int accountId) throws Exception {
        // ownership check
        final AccountRecord acct = bankDao.getById(accountId);
        if (acct == null || acct.userId != userId) {
            sendError(exchange, 404, "Account not found");
            return;
        }

        final TransactionRequest req;
        try {
            req = fromJson(exchange, TransactionRequest.class);
        } catch (JsonSyntaxException jse) {
            sendError(exchange, 400, "Malformed JSON body");
            return;
        }
        if (req == null || req.getType() == null || req.getAmount() == null) {
            sendError(exchange, 400, "type and amount are required");
            return;
        }

        final String type = req.getType().trim().toUpperCase();
        final BigDecimal amount;
        try {
            amount = new BigDecimal(req.getAmount().trim()).setScale(2, RoundingMode.UNNECESSARY);
            if (amount.signum() <= 0) throw new IllegalArgumentException();
        } catch (Exception ex) {
            sendError(exchange, 400, "amount must be a positive decimal with 2dp");
            return;
        }

        final TxRecord tx;
        switch (type) {
            case "DEPOSIT" -> tx = txDao.deposit(accountId, amount);
            case "WITHDRAW" -> tx = txDao.withdraw(accountId, amount);
            default -> {
                sendError(exchange, 400, "type must be DEPOSIT or WITHDRAW");
                return;
            }
        }

        if (tx == null) {
            // insufficient funds or concurrency issue
            sendError(exchange, 409, "Transaction failed");
            return;
        }

        final TransactionResponse resp = new TransactionResponse(
                tx.id,
                tx.type,
                tx.amount.setScale(2, RoundingMode.DOWN).toPlainString(),
                tx.balanceAfter.setScale(2, RoundingMode.DOWN).toPlainString(),
                tx.createdAt.toInstant().atOffset(ZoneOffset.UTC).format(ISO)
        );
        setLocation(exchange, "/users/me/accounts/" + accountId + "/transactions" );
        sendJson(exchange, 201, resp);
    }

    private void handleTxList(HttpExchange exchange, int userId, int accountId) throws Exception {
        // ownership check
        final AccountRecord acct = bankDao.getById(accountId);
        if (acct == null || acct.userId != userId) {
            sendError(exchange, 404, "Account not found");
            return;
        }

        // pagination params: limit (1..200), offset (>=0)
        final var query = exchange.getRequestURI().getQuery();
        int limit = clamp(parseIntParam(query, "limit", 100), 1, 200);
        int offset = Math.max(parseIntParam(query, "offset", 0), 0);

        final List<TxRecord> rows = txDao.listByAccountId(accountId, limit, offset);
        final List<TransactionResponse> out = new ArrayList<>();
        for (final TxRecord t : rows) {
            out.add(new TransactionResponse(
                    t.id,
                    t.type,
                    t.amount.setScale(2, RoundingMode.DOWN).toPlainString(),
                    t.balanceAfter.setScale(2, RoundingMode.DOWN).toPlainString(),
                    t.createdAt.toInstant().atOffset(ZoneOffset.UTC).format(ISO)
            ));
        }
        sendJson(exchange, 200, out);
    }

    // ========= helpers =========

    private static boolean isJson(HttpExchange ex) {
        final String ct = ex.getRequestHeaders().getFirst("Content-Type");
        if (ct == null) return false;
        // tolerate charset parameter
        final String mime = ct.split(";", 2)[0].trim();
        return "application/json".equalsIgnoreCase(mime);
    }

    private static String parseBearer(String header) {
        if (header == null) return null;
        if (!header.regionMatches(true, 0, "Bearer ", 0, 7)) return null;
        final String t = header.substring(7).trim();
        return t.isEmpty() ? null : t;
    }

    /** Holds parsed route info for /users/me/accounts and /users/me/accounts/{id}[/transactions] */
    private static class RouteInfo {
        Integer accountId;
        boolean isTransactions;
    }

    private static RouteInfo parseRoute(URI uri) {
        final RouteInfo r = new RouteInfo();
        String path = Objects.requireNonNullElse(uri.getPath(), "");
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        final String[] parts = path.split("/");
        // ['', 'users', 'me', 'accounts']                        -> collection
        // ['', 'users', 'me', 'accounts', '{id}']                -> single
        // ['', 'users', 'me', 'accounts', '{id}', 'transactions']-> tx endpoints

        if (parts.length >= 5) {
            try { r.accountId = Integer.parseInt(parts[4]); } catch (Exception ignore) { /* leave null */ }
        }
        r.isTransactions = (parts.length == 6 && "transactions".equalsIgnoreCase(parts[5]));
        return r;
    }

    private static void setLocation(HttpExchange ex, String path) {
        ex.getResponseHeaders().set("Location", path);
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
        if (allowHeader != null && !allowHeader.isEmpty()) {
            ex.getResponseHeaders().set("Allow", allowHeader);
        }
        ex.sendResponseHeaders(405, -1);
    }

    private static String allowedFor(RouteInfo r) {
        // Compute the Allow header depending on the matched sub-route
        if (r == null) return "";
        if (r.accountId == null) {
            // /users/me/accounts
            return "GET, POST, OPTIONS";
        }
        if (r.isTransactions) {
            // /users/me/accounts/{id}/transactions
            return "GET, POST, OPTIONS";
        }
        // /users/me/accounts/{id}
        return "GET, DELETE, OPTIONS";
    }

    private <T> T fromJson(HttpExchange exchange, Class<T> clazz) {
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int parseIntParam(String query, String key, int defaultVal) {
        if (query == null || query.isEmpty()) return defaultVal;
        String[] pairs = query.split("&");
        for (String p : pairs) {
            int i = p.indexOf('=');
            String k = i >= 0 ? p.substring(0, i) : p;
            if (k.equals(key)) {
                String v = i >= 0 ? p.substring(i + 1) : "";
                try { return Integer.parseInt(v); } catch (Exception ignore) { return defaultVal; }
            }
        }
        return defaultVal;
    }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
}
