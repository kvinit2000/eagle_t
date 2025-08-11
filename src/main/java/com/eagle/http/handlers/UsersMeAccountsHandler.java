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
import com.eagle.model.response.ErrorResponse;
import com.eagle.model.response.TransactionResponse;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Refactored to extend BaseHandler, with consistent JSON I/O, method/Allow handling,
 * Content-Type validation, pagination, and Location headers.
 */
public class UsersMeAccountsHandler extends BaseHandler {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    private final UserDao userDao = new UserDao();
    private final BankAccountDao bankDao = new BankAccountDao();
    private final TransactionDao txDao = new TransactionDao();

    @Override
    protected void doHandle(HttpExchange ex) throws Exception {
        RouteInfo route = parseRoute(ex.getRequestURI());
        String allow = allowedFor(route);

        switch (ex.getRequestMethod()) {
            case "GET" -> handleGet(ex, route);
            case "POST" -> handlePost(ex, route, allow);
            case "DELETE" -> handleDelete(ex, route, allow);
            case "OPTIONS" -> { ex.getResponseHeaders().set("Allow", allow); ex.sendResponseHeaders(204, -1); }
            default -> { ex.getResponseHeaders().set("Allow", allow); ex.sendResponseHeaders(405, -1); }
        }
    }

    // ========= dispatchers =========

    private void handleGet(HttpExchange ex, RouteInfo r) throws Exception {
        String token = parseBearer(ex.getRequestHeaders().getFirst("Authorization"));
        if (token == null) { writeJson(ex, 401, new ErrorResponse("unauthorized", "Missing or invalid Authorization header")); return; }
        UserRecord me = userDao.getUserByAuthToken(token);
        if (me == null) { writeJson(ex, 401, new ErrorResponse("unauthorized", "Invalid token")); return; }

        if (r.accountId == null && !r.isTransactions) {
            listAccounts(ex, me.id);
        } else if (r.accountId != null && !r.isTransactions) {
            getOneAccount(ex, me.id, r.accountId);
        } else if (r.accountId != null && r.isTransactions) {
            listTransactions(ex, me.id, r.accountId);
        } else {
            methodNotAllowed(ex, r);
        }
    }

    private void handlePost(HttpExchange ex, RouteInfo r, String allow) throws Exception {
        if (!isJsonRequest(ex)) { writeJson(ex, 415, new ErrorResponse("unsupported_media_type", "Content-Type must be application/json")); return; }

        String token = parseBearer(ex.getRequestHeaders().getFirst("Authorization"));
        if (token == null) { writeJson(ex, 401, new ErrorResponse("unauthorized", "Missing or invalid Authorization header")); return; }
        UserRecord me = userDao.getUserByAuthToken(token);
        if (me == null) { writeJson(ex, 401, new ErrorResponse("unauthorized", "Invalid token")); return; }

        if (r.accountId == null && !r.isTransactions) {
            createAccount(ex, me.id);
        } else if (r.accountId != null && r.isTransactions) {
            createTransaction(ex, me.id, r.accountId);
        } else {
            ex.getResponseHeaders().set("Allow", allow); ex.sendResponseHeaders(405, -1);
        }
    }

    private void handleDelete(HttpExchange ex, RouteInfo r, String allow) throws Exception {
        String token = parseBearer(ex.getRequestHeaders().getFirst("Authorization"));
        if (token == null) { writeJson(ex, 401, new ErrorResponse("unauthorized", "Missing or invalid Authorization header")); return; }
        UserRecord me = userDao.getUserByAuthToken(token);
        if (me == null) { writeJson(ex, 401, new ErrorResponse("unauthorized", "Invalid token")); return; }

        if (r.accountId != null && !r.isTransactions) {
            deleteAccount(ex, me.id, r.accountId);
        } else {
            ex.getResponseHeaders().set("Allow", allow); ex.sendResponseHeaders(405, -1);
        }
    }

    // ========= Accounts =========

    private void createAccount(HttpExchange ex, int userId) throws Exception {
        CreateAccountRequest req = readJson(ex, CreateAccountRequest.class);
        String desired = (req != null && req.getAccountNumber() != null && !req.getAccountNumber().trim().isEmpty())
                ? req.getAccountNumber().trim() : null;

        AccountRecord r = bankDao.createForUser(userId, desired);
        if (r == null) { writeJson(ex, 409, Map.of("ok", false, "message", "Could not create account")); return; }

        AccountResponse resp = new AccountResponse(
                r.id,
                r.accountNumber,
                (r.balance == null ? "0.00" : r.balance.setScale(2, RoundingMode.DOWN).toPlainString())
        );
        ex.getResponseHeaders().set("Location", "/users/me/accounts/" + r.id);
        writeJson(ex, 201, resp);
    }

    private void listAccounts(HttpExchange ex, int userId) throws Exception {
        List<AccountRecord> rows = bankDao.listByUserId(userId);
        List<AccountResponse> out = new ArrayList<>();
        for (AccountRecord r : rows) {
            out.add(new AccountResponse(
                    r.id,
                    r.accountNumber,
                    (r.balance == null ? "0.00" : r.balance.setScale(2, RoundingMode.DOWN).toPlainString())
            ));
        }
        writeJson(ex, 200, out);
    }

    private void getOneAccount(HttpExchange ex, int userId, int accountId) throws Exception {
        AccountRecord r = bankDao.getById(accountId);
        if (r == null || r.userId != userId) { writeJson(ex, 404, Map.of("ok", false, "message", "Account not found")); return; }
        AccountResponse resp = new AccountResponse(
                r.id,
                r.accountNumber,
                (r.balance == null ? "0.00" : r.balance.setScale(2, RoundingMode.DOWN).toPlainString())
        );
        writeJson(ex, 200, resp);
    }

    private void deleteAccount(HttpExchange ex, int userId, int accountId) throws Exception {
        AccountRecord r = bankDao.getById(accountId);
        if (r == null || r.userId != userId) { writeJson(ex, 404, Map.of("ok", false, "message", "Account not found")); return; }
        if (r.balance == null || r.balance.signum() != 0) {
            writeJson(ex, 409, Map.of("ok", false, "message", "Balance must be zero before deletion", "balance", r.balance == null ? "unknown" : r.balance.toPlainString()));
            return;
        }
        boolean deleted = bankDao.deleteIfZeroBalance(accountId, userId);
        if (!deleted) { writeJson(ex, 409, Map.of("ok", false, "message", "Could not delete account (maybe balance changed)")); return; }
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(204, -1);
    }

    // ========= Transactions =========

    private void createTransaction(HttpExchange ex, int userId, int accountId) throws Exception {
        AccountRecord acct = bankDao.getById(accountId);
        if (acct == null || acct.userId != userId) { writeJson(ex, 404, Map.of("ok", false, "message", "Account not found")); return; }

        TransactionRequest req;
        try { req = readJson(ex, TransactionRequest.class); }
        catch (JsonSyntaxException jse) { writeJson(ex, 400, new ErrorResponse("bad_request", "Malformed JSON body")); return; }
        if (req == null || req.getType() == null || req.getAmount() == null) { writeJson(ex, 400, new ErrorResponse("bad_request", "type and amount are required")); return; }

        String type = req.getType().trim().toUpperCase();
        BigDecimal amount;
        try {
            amount = new BigDecimal(req.getAmount().trim()).setScale(2, RoundingMode.UNNECESSARY);
            if (amount.signum() <= 0) throw new IllegalArgumentException();
        } catch (Exception ex2) {
            writeJson(ex, 400, new ErrorResponse("bad_request", "amount must be a positive decimal with 2dp"));
            return;
        }

        TxRecord tx;
        switch (type) {
            case "DEPOSIT" -> tx = txDao.deposit(accountId, amount);
            case "WITHDRAW" -> tx = txDao.withdraw(accountId, amount);
            default -> { writeJson(ex, 400, new ErrorResponse("bad_request", "type must be DEPOSIT or WITHDRAW")); return; }
        }

        if (tx == null) { writeJson(ex, 409, new ErrorResponse("conflict", "Transaction failed")); return; }

        TransactionResponse resp = new TransactionResponse(
                tx.id,
                tx.type,
                tx.amount.setScale(2, RoundingMode.DOWN).toPlainString(),
                tx.balanceAfter.setScale(2, RoundingMode.DOWN).toPlainString(),
                tx.createdAt.toInstant().atOffset(ZoneOffset.UTC).format(ISO)
        );
        ex.getResponseHeaders().set("Location", "/users/me/accounts/" + accountId + "/transactions");
        writeJson(ex, 201, resp);
    }

    private void listTransactions(HttpExchange ex, int userId, int accountId) throws Exception {
        AccountRecord acct = bankDao.getById(accountId);
        if (acct == null || acct.userId != userId) { writeJson(ex, 404, Map.of("ok", false, "message", "Account not found")); return; }

        String q = ex.getRequestURI().getQuery();
        int limit = clamp(parseIntParam(q, "limit", 100), 1, 200);
        int offset = Math.max(parseIntParam(q, "offset", 0), 0);

        List<TxRecord> rows = txDao.listByAccountId(accountId, limit, offset);
        List<TransactionResponse> out = new ArrayList<>();
        for (TxRecord t : rows) {
            out.add(new TransactionResponse(
                    t.id,
                    t.type,
                    t.amount.setScale(2, RoundingMode.DOWN).toPlainString(),
                    t.balanceAfter.setScale(2, RoundingMode.DOWN).toPlainString(),
                    t.createdAt.toInstant().atOffset(ZoneOffset.UTC).format(ISO)
            ));
        }
        writeJson(ex, 200, out);
    }

    // ========= helpers =========

    /** Holds parsed route info for /users/me/accounts and /users/me/accounts/{id}[/transactions] */
    private static class RouteInfo { Integer accountId; boolean isTransactions; }

    private static RouteInfo parseRoute(URI uri) {
        RouteInfo r = new RouteInfo();
        String path = Objects.requireNonNullElse(uri.getPath(), "");
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        String[] parts = path.split("/");
        if (parts.length >= 5) { try { r.accountId = Integer.parseInt(parts[4]); } catch (Exception ignore) {} }
        r.isTransactions = (parts.length == 6 && "transactions".equalsIgnoreCase(parts[5]));
        return r;
    }

    private static String allowedFor(RouteInfo r) {
        if (r == null) return "GET, POST, DELETE, OPTIONS";
        if (r.accountId == null) return "GET, POST, OPTIONS"; // collection
        if (r.isTransactions) return "GET, POST, OPTIONS";     // nested tx
        return "GET, DELETE, OPTIONS";                          // single
    }

    private static void methodNotAllowed(HttpExchange ex, RouteInfo r) throws Exception {
        ex.getResponseHeaders().set("Allow", allowedFor(r));
        ex.sendResponseHeaders(405, -1);
    }

    private static int parseIntParam(String query, String key, int def) {
        if (query == null || query.isEmpty()) return def;
        for (String p : query.split("&")) {
            int i = p.indexOf('=');
            String k = i >= 0 ? p.substring(0, i) : p;
            if (k.equals(key)) {
                String v = i >= 0 ? p.substring(i + 1) : "";
                try { return Integer.parseInt(v); } catch (Exception ignore) { return def; }
            }
        }
        return def;
    }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
}
