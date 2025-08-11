package com.eagle.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

public class AccountsClient extends AbsClient {
    private static final Gson GSON = new Gson();

    // ---- Accounts ----
    public String create(String baseUrl, String token, String accountNumber) throws Exception {
        HttpURLConnection c = open(joinUrl(baseUrl, "/users/me/accounts"), "POST");
        setAuth(c, token); setJsonHeaders(c); enableBody(c);
        String json = (accountNumber == null || accountNumber.isBlank()) ? "{}"
                : "{\"accountNumber\":\"" + escape(accountNumber) + "\"}";
        writeJson(c, json);
        int code = c.getResponseCode();
        String body = readBody(c, code);
        String loc = c.getHeaderField("Location");
        return (loc==null||loc.isBlank())? ("HTTP "+code+" -> "+body) : ("HTTP "+code+" Location: "+loc+" -> "+body);
    }

    public String list(String baseUrl, String token) throws Exception {
        HttpURLConnection c = open(joinUrl(baseUrl, "/users/me/accounts"), "GET");
        setAuth(c, token); c.setRequestProperty("Accept","application/json");
        int code = c.getResponseCode();
        String body = readBody(c, code);
        return "HTTP "+code+" -> "+body;
    }

    public String getOne(String baseUrl, String token, int id) throws Exception {
        HttpURLConnection c = open(joinUrl(baseUrl, "/users/me/accounts/"+id), "GET");
        setAuth(c, token); c.setRequestProperty("Accept","application/json");
        int code = c.getResponseCode();
        String body = readBody(c, code);
        return "HTTP "+code+" -> "+body;
    }

    public String delete(String baseUrl, String token, int id) throws Exception {
        HttpURLConnection c = open(joinUrl(baseUrl, "/users/me/accounts/"+id), "DELETE");
        setAuth(c, token); c.setRequestProperty("Accept","application/json");
        int code = c.getResponseCode();
        String body = readBody(c, code);
        return "HTTP "+code+(body.isBlank()?"":" -> "+body);
    }

    // ---- Transactions ----
    public String deposit(String baseUrl, String token, int accountId, String amount) throws Exception {
        return tx(baseUrl, token, accountId, "DEPOSIT", amount);
    }
    public String withdraw(String baseUrl, String token, int accountId, String amount) throws Exception {
        return tx(baseUrl, token, accountId, "WITHDRAW", amount);
    }

    public String listTransactions(String baseUrl, String token, int accountId, Integer limit, Integer offset) throws Exception {
        StringBuilder qs = new StringBuilder();
        if (limit != null) qs.append(qs.length()==0?"?":"&").append("limit=").append(Math.max(1, Math.min(200, limit)));
        if (offset != null) qs.append(qs.length()==0?"?":"&").append("offset=").append(Math.max(0, offset));
        HttpURLConnection c = open(joinUrl(baseUrl, "/users/me/accounts/"+accountId+"/transactions"+qs), "GET");
        setAuth(c, token); c.setRequestProperty("Accept","application/json");
        int code = c.getResponseCode();
        String body = readBody(c, code);
        return "HTTP "+code+" -> "+body;
    }

    public String listTransactions(String baseUrl, String token, int accountId) throws Exception {
        return listTransactions(baseUrl, token, accountId, null, null);
    }

    // helper to parse first id from list()
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

    private static String escape(String s){return s==null?"":s.replace("\\","\\\\").replace("\"","\\\"");}

    private String tx(String baseUrl, String token, int accountId, String type, String amount) throws Exception {
        HttpURLConnection c = open(joinUrl(baseUrl, "/users/me/accounts/"+accountId+"/transactions"), "POST");
        setAuth(c, token); setJsonHeaders(c); enableBody(c);
        String json = "{\"type\":\""+type+"\",\"amount\":\""+escape(amount)+"\"}";
        writeJson(c, json);
        int code = c.getResponseCode();
        String body = readBody(c, code);
        String loc = c.getHeaderField("Location");
        return (loc==null||loc.isBlank())? ("HTTP "+code+" -> "+body) : ("HTTP "+code+" Location: "+loc+" -> "+body);
    }
}