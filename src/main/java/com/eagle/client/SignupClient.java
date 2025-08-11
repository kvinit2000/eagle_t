package com.eagle.client;

import com.eagle.model.request.SignupRequest;
import com.google.gson.Gson;

import java.net.HttpURLConnection;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class SignupClient extends AbsClient {
    public static final Map<String, String> USERS = new LinkedHashMap<>(); // username -> password
    public static final Map<String, String> TOKENS = new LinkedHashMap<>(); // username -> bearer token

    private static final Random RAND = new Random();
    private static final Gson GSON = new Gson();

    public static volatile String LAST_USERNAME = null;
    public static volatile String LAST_PASSWORD = null;
    public static volatile String LAST_AUTH_TOKEN = null;

    public static String signupRandom() throws Exception { return new SignupClient().signupRandom(DEFAULT_BASE_URL); }
    public String signupRandom(String baseUrl) throws Exception {
        String username = "user" + System.currentTimeMillis() + RAND.nextInt(1000);
        String password = "pass" + RAND.nextInt(1000);
        String email = username + "@example.com";
        String dob = LocalDate.of(1990 + RAND.nextInt(20), 1 + RAND.nextInt(12), 1 + RAND.nextInt(28)).toString();
        String address = "123 Main Street";
        String pin = String.format("%06d", RAND.nextInt(999_999));
        String phone = "+44" + (100000000 + RAND.nextInt(899_999_999));
        return signupAndStore(baseUrl, username, password, email, dob, address, pin, phone);
    }

    public String signupAndStore(String baseUrl, String username, String password,
                                 String email, String dob, String address,
                                 String pin, String phone) throws Exception {
        SignupRequest req = new SignupRequest(username, password, email, dob, address, pin, phone);
        String respJson = signup(baseUrl, req);
        try {
            @SuppressWarnings("unchecked") Map<String,Object> map = GSON.fromJson(respJson, Map.class);
            String serverUsername = asString(map.get("username"));
            String authToken = asString(map.get("authToken"));
            if (serverUsername != null && !serverUsername.isBlank()) {
                USERS.remove(username); USERS.put(serverUsername, password);
                if (authToken != null && !authToken.isBlank()) { TOKENS.put(serverUsername, authToken); LAST_AUTH_TOKEN = authToken; }
                LAST_USERNAME = serverUsername; LAST_PASSWORD = password; return respJson;
            }
        } catch (Exception ignore) { /* fall back below */ }
        USERS.put(username, password); LAST_USERNAME = username; LAST_PASSWORD = password; return respJson;
    }

    public static String signupAndStore(String username, String password) throws Exception {
        return new SignupClient().signupAndStore(DEFAULT_BASE_URL, username, password, null, null, null, null, null);
    }

    public String signup(String baseUrl, SignupRequest req) throws Exception {
        HttpURLConnection c = open(joinUrl(baseUrl, "/signup"), "POST");
        setJsonHeaders(c); enableBody(c);
        writeJson(c, req.toJson());
        int code = c.getResponseCode();
        return readBody(c, code);
    }

    public static String bearerFor(String username) {
        String tok = TOKENS.get(username);
        return (tok == null || tok.isBlank()) ? null : "Bearer " + tok;
    }

    private static String asString(Object o){ return (o==null)?null:(o instanceof String s ? s : String.valueOf(o)); }
}