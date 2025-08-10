package com.eagle.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class AccountsClient {
    private static final Gson GSON = new Gson();

    // ========= Accounts =========

    /** POST /users/me/accounts — create account. If accountNumber is null/blank, server generates one. */
    public static String create(String token, String accountNumber) throws Exception {
        URL url = new URL("http://localhost:8080/users/me/accounts");
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Authorization", "Bearer " + token);
        c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        String json = (accountNumber == null || accountNumber.isBlank())
                ? "{}"
                : "{\"accountNumber\":\"" + escape(accountNumber) + "\"}";
        try (OutputStream os = c.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        int code = c.getResponseCode();
        String body = readBody(c, code);
        return "HTTP " + code + " -> " + body;
    }

    /** GET /users/me/accounts — list my accounts. */
    public static String list(String token) throws Exception {
        URL url = new URL("http://localhost:8080/users/me/accounts");
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("GET");
        c.setRequestProperty("Authorization", "Bearer " + token);

        int code = c.getResponseCode();
        String body = readBody(c, code);
        return "HTTP " + code + " -> " + body;
    }

    /** GET /users/me/accounts/{id} — get account details (ownership enforced). */
    public static String getOne(String token, int id) throws Exception {
        URL url = new URL("http://localhost:8080/users/me/accounts/" + id);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("GET");
        c.setRequestProperty("Authorization", "Bearer " + token);

        int code = c.getResponseCode();
        String body = readBody(c, code);
        return "HTTP " + code + " -> " + body;
    }

    /** DELETE /users/me/accounts/{id} — delete account (requires balance == 0). */
    public static String delete(String token, int id) throws Exception {
        URL url = new URL("http://localhost:8080/users/me/accounts/" + id);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("DELETE");
        c.setRequestProperty("Authorization", "Bearer " + token);

        int code = c.getResponseCode();
        String body = readBody(c, code);
        return "HTTP " + code + (body.isBlank() ? "" : " -> " + body);
    }

    // ========= Transactions =========

    /** POST /users/me/accounts/{id}/transactions — deposit amount (e.g., "25.00"). */
    public static String deposit(String token, int accountId, String amount) throws Exception {
        return tx(token, accountId, "DEPOSIT", amount);
    }

    /** POST /users/me/accounts/{id}/transactions — withdraw amount (e.g., "10.00"). */
    public static String withdraw(String token, int accountId, String amount) throws Exception {
        return tx(token, accountId, "WITHDRAW", amount);
    }

    /** GET /users/me/accounts/{id}/transactions — list newest-first. */
    public static String listTransactions(String token, int accountId) throws Exception {
        URL url = new URL("http://localhost:8080/users/me/accounts/" + accountId + "/transactions");
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("GET");
        c.setRequestProperty("Authorization", "Bearer " + token);

        int code = c.getResponseCode();
        String body = readBody(c, code);
        return "HTTP " + code + " -> " + body;
    }

    // ========= Helpers =========

    /** Convenience: parse the first account id from list() JSON result to drive demos. */
    public static Integer firstIdFromList(String listResponse) {
        try {
            int idx = listResponse.indexOf("->");
            String json = (idx >= 0) ? listResponse.substring(idx + 2).trim() : listResponse.trim();
            Type t = TypeToken.getParameterized(List.class, Map.class).getType();
            List<Map<String, Object>> arr = GSON.fromJson(json, t);
            if (arr == null || arr.isEmpty()) return null;
            Object id = arr.get(0).get("id");
            if (id instanceof Number n) return n.intValue();
            if (id instanceof String s) return Integer.parseInt(s);
        } catch (Exception ignore) {}
        return null;
    }

    private static String tx(String token, int accountId, String type, String amount) throws Exception {
        URL url = new URL("http://localhost:8080/users/me/accounts/" + accountId + "/transactions");
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Authorization", "Bearer " + token);
        c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        String json = "{\"type\":\"" + type + "\",\"amount\":\"" + escape(amount) + "\"}";
        try (OutputStream os = c.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        int code = c.getResponseCode();
        String body = readBody(c, code);
        return "HTTP " + code + " -> " + body;
    }

    private static String readBody(HttpURLConnection c, int code) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                (code >= 200 && code < 300) ? c.getInputStream() : c.getErrorStream(),
                StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            for (String l; (l = br.readLine()) != null; ) sb.append(l);
            return sb.toString();
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        // minimal JSON string escape for quotes/backslashes; good enough for these payloads
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
