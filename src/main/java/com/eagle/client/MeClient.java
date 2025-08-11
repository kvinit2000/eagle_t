package com.eagle.client;

import com.eagle.model.response.UserProfileResponse;
import com.google.gson.Gson;

import java.net.HttpURLConnection;
import java.util.LinkedHashMap;
import java.util.Map;

public class MeClient extends AbsClient {
    private static final Gson GSON = new Gson();

    public UserProfileResponse getMe(String baseUrl, String authToken) throws Exception {
        requireToken(authToken);
        HttpURLConnection c = open(joinUrl(baseUrl, "/users/me"), "GET");
        setAuth(c, authToken);
        c.setRequestProperty("Accept", "application/json");
        int code = c.getResponseCode();
        String body = readBody(c, code);
        if (code >= 200 && code < 300) return (body.isBlank()? null : GSON.fromJson(body, UserProfileResponse.class));
        throw new RuntimeException("HTTP "+code+" GET /users/me -> "+body);
    }
    public static UserProfileResponse getMe(String authToken) throws Exception {
        return new MeClient().getMe(DEFAULT_BASE_URL, authToken);
    }

    public UserProfileResponse patch(String baseUrl, String authToken, Map<String, ?> fields) throws Exception {
        requireToken(authToken);
        Map<String,Object> payload = new LinkedHashMap<>();
        if (fields != null) {
            for (Map.Entry<String,?> e : fields.entrySet()) {
                Object v = e.getValue();
                if (v == null) continue;
                if (v instanceof String s) { s = s.trim(); if (s.isEmpty()) continue; payload.put(e.getKey(), s); }
                else payload.put(e.getKey(), v);
            }
        }
        if (payload.isEmpty()) throw new IllegalArgumentException("No fields to update (payload would be empty).");

        HttpURLConnection c = open(joinUrl(baseUrl, "/users/me"), "POST");
        setPatchOrOverride(c); // PATCH or override
        setAuth(c, authToken); setJsonHeaders(c); enableBody(c);
        writeJson(c, new Gson().toJson(payload));
        int code = c.getResponseCode();
        String body = readBody(c, code);
        if (code >= 200 && code < 300) return (body.isBlank()? null : GSON.fromJson(body, UserProfileResponse.class));
        throw new RuntimeException("HTTP "+code+" PATCH /users/me -> "+body);
    }

    public UserProfileResponse patchMe(String baseUrl, String authToken,
                                       String email, String dob, String address, String pin, String phone) throws Exception {
        Map<String,Object> payload = new LinkedHashMap<>();
        if (!isBlank(email)) payload.put("email", email.trim());
        if (!isBlank(dob)) payload.put("dob", dob.trim());
        if (!isBlank(address)) payload.put("address", address.trim());
        if (!isBlank(pin)) payload.put("pin", pin.trim());
        if (!isBlank(phone)) payload.put("phone", phone.trim());
        return patch(baseUrl, authToken, payload);
    }
    public static UserProfileResponse patchMe(String authToken,
                                              String email,String dob,String address,String pin,String phone) throws Exception {
        return new MeClient().patchMe(DEFAULT_BASE_URL, authToken, email, dob, address, pin, phone);
    }

    private static void requireToken(String token){ if (token==null || token.isBlank()) throw new IllegalArgumentException("authToken is required"); }
    private static boolean isBlank(String s){ return s==null || s.trim().isEmpty(); }
}