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

    public static String create(String token, String accountNumber) throws Exception {
        URL url = new URL("http://localhost:8080/users/me/accounts");
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setRequestProperty("Authorization", "Bearer " + token);
        c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        String json = (accountNumber == null || accountNumber.isBlank())
                ? "{}" : "{\"accountNumber\":\"" + accountNumber + "\"}";
        try (OutputStream os = c.getOutputStream()) { os.write(json.getBytes(StandardCharsets.UTF_8)); }

        int code = c.getResponseCode();
        String body = readBody(c, code);
        return "HTTP " + code + " -> " + body;
    }

    public static String list(String token) throws Exception {
        URL url = new URL("http://localhost:8080/users/me/accounts");
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("GET");
        c.setRequestProperty("Authorization", "Bearer " + token);

        int code = c.getResponseCode();
        String body = readBody(c, code);
        return "HTTP " + code + " -> " + body;
    }

    public static String getOne(String token, int id) throws Exception {
        URL url = new URL("http://localhost:8080/users/me/accounts/" + id);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("GET");
        c.setRequestProperty("Authorization", "Bearer " + token);

        int code = c.getResponseCode();
        String body = readBody(c, code);
        return "HTTP " + code + " -> " + body;
    }

    public static String delete(String token, int id) throws Exception {
        URL url = new URL("http://localhost:8080/users/me/accounts/" + id);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("DELETE");
        c.setRequestProperty("Authorization", "Bearer " + token);

        int code = c.getResponseCode();
        String body = readBody(c, code);
        return "HTTP " + code + (body.isBlank() ? "" : " -> " + body);
    }

    // Parse the first id from list JSON: [{"id":1,"accountNumber":"...","balance":"0.00"}, ...]
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

    private static String readBody(HttpURLConnection c, int code) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                (code >= 200 && code < 300) ? c.getInputStream() : c.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            for (String l; (l = br.readLine()) != null; ) sb.append(l);
            return sb.toString();
        }
    }
}
