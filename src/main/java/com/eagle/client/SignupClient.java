package com.eagle.client;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SignupClient {

    // Public so other clients (like MasterClient) can access
    public static final Map<String, String> USERS = new LinkedHashMap<>();
    private static final Random RAND = new Random();

    /** Generate a random username/password, signup, and store in USERS */
    public static String signupRandom() throws Exception {
        String username = "user" + System.currentTimeMillis() + RAND.nextInt(1000);
        String password = "pass" + RAND.nextInt(1000);

        USERS.put(username, password); // store for reuse
        return signup(username, password);
    }

    /** Actual signup request */
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
