package com.eagle.client;

import com.google.gson.Gson;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SignupClient {

    // Public so MasterClient can use all accumulated creds
    public static final Map<String, String> USERS = new LinkedHashMap<>();
    private static final Random RAND = new Random();
    private static final Gson GSON = new Gson();

    // Keep track of last created (handy for quick login)
    public static volatile String LAST_USERNAME = null;
    public static volatile String LAST_PASSWORD = null;

    /** Generate a random username/password, signup, and store exact server username in USERS */
    public static String signupRandom() throws Exception {
        String username = "user" + System.currentTimeMillis() + RAND.nextInt(1000);
        String password = "pass" + RAND.nextInt(1000);
        return signupAndStore(username, password);
    }

    /** Call /signup with explicit creds and store EXACT username the server returns */
    public static String signupAndStore(String username, String password) throws Exception {
        String respJson = signup(username, password);

        // Try to read {"username": "..."} from server response
        try {
            Map<?,?> map = GSON.fromJson(respJson, Map.class);
            Object serverUsername = (map != null) ? map.get("username") : null;
            if (serverUsername instanceof String su && !su.isBlank()) {
                // In case server changed the username, reflect that in our map
                USERS.remove(username);
                USERS.put(su, password);
                LAST_USERNAME = su;
                LAST_PASSWORD = password;
                return respJson;
            }
        } catch (Exception ignore) {
            // If parsing fails, fall back to client-generated
        }
        // If server didn't return username, assume it saved what we sent
        USERS.put(username, password);
        LAST_USERNAME = username;
        LAST_PASSWORD = password;
        return respJson;
    }

    /** Raw HTTP call to /signup (no storage) */
    public static String signup(String username, String password) throws Exception {
        URL url = new URL("http://localhost:8080/signup");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        String jsonBody = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            for (String l; (l = br.readLine()) != null; ) sb.append(l);
            return sb.toString();
        }
    }
}
