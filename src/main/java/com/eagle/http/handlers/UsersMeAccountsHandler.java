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
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

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

public class UsersMeAccountsHandler implements HttpHandler {
    private static final Gson GSON = new Gson();
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    private final UserDao userDao = new UserDao();
    private final BankAccountDao bankDao = new BankAccountDao();
    private final TransactionDao txDao = new TransactionDao();

    @Override
    public void handle(HttpExchange exchange) {
        try {
            String method = exchange.getRequestMethod();

            // Auth: Authorization: Bearer <token>
            String token = parseBearer(exchange.getRequestHeaders().getFirst("Authorization"));
            if (token == null) {
                writeJson(exchange, 401, Map.of("ok", false, "message", "Missing or invalid Authorization header"));
                return;
            }
            UserRecord me = userDao.getUserByAuthToken(token);
            if (me == null) {
                writeJson(exchange, 401, Map.of("ok", false, "message", "Invalid token"));
                return;
            }

            // Route parsing
            var route = parseRoute(exchange.getRequestURI()); // tells us accountId and whether it's /transactions
            Integer accountId = route.accountId;
            boolean isTx = route.isTransactions;

            switch (method) {
                case "POST" -> {
                    if (isTx && accountId != null) {
                        handleTxCreate(exchange, me.id, accountId);
                    } else if (!isTx && accountId == null) {
                        handleCreate(exchange, me.id);
                    } else {
                        exchange.sendResponseHeaders(405, -1);
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
                        exchange.sendResponseHeaders(405, -1);
                    }
                }
                case "DELETE" -> {
                    if (!isTx && accountId != null) {
                        handleDelete(exchange, me.id, accountId);
                    } else {
                        exchange.sendResponseHeaders(405, -1);
                    }
                }
                default -> exchange.sendResponseHeaders(405, -1);
            }
        } catch (Exception e) {
            try { writeJson(exchange, 500, Map.of("ok", false, "message", "Server error")); }
            catch (Exception ignore) {}
        }
    }

    // ========= Accounts =========

    private void handleCreate(HttpExchange exchange, int userId) throws Exception {
        CreateAccountRequest req = GSON.fromJson(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8),
                CreateAccountRequest.class
        );
        String desired = (req != null && req.getAccountNumber() != null && !req.getAccountNumber().trim().isEmpty())
                ? req.getAccountNumber().trim()
                : null;

        AccountRecord r = bankDao.createForUser(userId, desired);
        if (r == null) {
            writeJson(exchange, 409, Map.of("ok", false, "message", "Could not create account"));
            return;
        }

        AccountResponse resp = new AccountResponse(
                r.id,
                r.accountNumber,
                (r.balance == null ? "0.00" : r.balance.setScale(2, RoundingMode.DOWN).toPlainString())
        );
        writeJson(exchange, 201, resp);
    }

    private void handleList(HttpExchange exchange, int userId) throws Exception {
        List<AccountRecord> rows = bankDao.listByUserId(userId);
        List<AccountResponse> out = new ArrayList<>();
        for (AccountRecord r : rows) {
            out.add(new AccountResponse(
                    r.id,
                    r.accountNumber,
                    (r.balance == null ? "0.00" : r.balance.setScale(2, RoundingMode.DOWN).toPlainString())
            ));
        }
        writeJson(exchange, 200, out);
    }

    private void handleGetOne(HttpExchange exchange, int userId, int accountId) throws Exception {
        AccountRecord r = bankDao.getById(accountId);
        if (r == null || r.userId != userId) {
            writeJson(exchange, 404, Map.of("ok", false, "message", "Account not found"));
            return;
        }
        AccountResponse resp = new AccountResponse(
                r.id,
                r.accountNumber,
                (r.balance == null ? "0.00" : r.balance.setScale(2, RoundingMode.DOWN).toPlainString())
        );
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

    // ========= Transactions =========

    private void handleTxCreate(HttpExchange exchange, int userId, int accountId) throws Exception {
        // ownership check
        AccountRecord acct = bankDao.getById(accountId);
        if (acct == null || acct.userId != userId) {
            writeJson(exchange, 404, Map.of("ok", false, "message", "Account not found"));
            return;
        }

        TransactionRequest req = GSON.fromJson(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8),
                TransactionRequest.class
        );
        if (req == null || req.getType() == null || req.getAmount() == null) {
            writeJson(exchange, 400, Map.of("ok", false, "message", "type and amount are required"));
            return;
        }

        String type = req.getType().trim().toUpperCase();
        BigDecimal amount;
        try {
            amount = new BigDecimal(req.getAmount().trim()).setScale(2, RoundingMode.UNNECESSARY);
            if (amount.signum() <= 0) throw new IllegalArgumentException();
        } catch (Exception ex) {
            writeJson(exchange, 400, Map.of("ok", false, "message", "amount must be a positive decimal with 2dp"));
            return;
        }

        TxRecord tx;
        switch (type) {
            case "DEPOSIT" -> tx = txDao.deposit(accountId, amount);
            case "WITHDRAW" -> tx = txDao.withdraw(accountId, amount);
            default -> {
                writeJson(exchange, 400, Map.of("ok", false, "message", "type must be DEPOSIT or WITHDRAW"));
                return;
            }
        }

        if (tx == null) {
            // insufficient funds or concurrency issue
            writeJson(exchange, 409, Map.of("ok", false, "message", "Transaction failed"));
            return;
        }

        TransactionResponse resp = new TransactionResponse(
                tx.id,
                tx.type,
                tx.amount.setScale(2, RoundingMode.DOWN).toPlainString(),
                tx.balanceAfter.setScale(2, RoundingMode.DOWN).toPlainString(),
                tx.createdAt.toInstant().atOffset(ZoneOffset.UTC).format(ISO)
        );
        writeJson(exchange, 201, resp);
    }

    private void handleTxList(HttpExchange exchange, int userId, int accountId) throws Exception {
        // ownership check
        AccountRecord acct = bankDao.getById(accountId);
        if (acct == null || acct.userId != userId) {
            writeJson(exchange, 404, Map.of("ok", false, "message", "Account not found"));
            return;
        }

        var rows = txDao.listByAccountId(accountId, 100, 0);
        List<TransactionResponse> out = new ArrayList<>();
        for (var t : rows) {
            out.add(new TransactionResponse(
                    t.id,
                    t.type,
                    t.amount.setScale(2, RoundingMode.DOWN).toPlainString(),
                    t.balanceAfter.setScale(2, RoundingMode.DOWN).toPlainString(),
                    t.createdAt.toInstant().atOffset(ZoneOffset.UTC).format(ISO)
            ));
        }
        writeJson(exchange, 200, out);
    }

    // ========= helpers =========

    private static String parseBearer(String header) {
        if (header == null) return null;
        if (!header.regionMatches(true, 0, "Bearer ", 0, 7)) return null;
        String t = header.substring(7).trim();
        return t.isEmpty() ? null : t;
    }

    /** Holds parsed route info for /users/me/accounts and /users/me/accounts/{id}[/transactions] */
    private static class RouteInfo {
        Integer accountId;
        boolean isTransactions;
    }

    private static RouteInfo parseRoute(URI uri) {
        RouteInfo r = new RouteInfo();
        String path = uri.getPath();
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        String[] parts = path.split("/");
        // ['', 'users', 'me', 'accounts']                        -> collection
        // ['', 'users', 'me', 'accounts', '{id}']                -> single
        // ['', 'users', 'me', 'accounts', '{id}', 'transactions']-> tx endpoints

        if (parts.length >= 5) {
            try { r.accountId = Integer.parseInt(parts[4]); } catch (Exception ignore) {}
        }
        r.isTransactions = (parts.length == 6 && "transactions".equalsIgnoreCase(parts[5]));
        return r;
    }

    private void writeJson(HttpExchange ex, int status, Object body) throws Exception {
        byte[] out = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, out.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(out); }
    }
}
