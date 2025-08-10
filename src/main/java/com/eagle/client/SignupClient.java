package com.eagle.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class SignupClient {
    public static String signupRandom() throws Exception {
        String username = "user_" + UUID.randomUUID().toString().substring(0, 5);
        String password = "pass123";
        String jsonPayload = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        return signup(jsonPayload);
    }

    public static String signup(String jsonPayload) throws Exception {
        URL url = new URL("http://localhost:8080/signup");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonPayload.getBytes());
            os.flush();
        }
        return readResponse(conn);
    }

    private static String readResponse(HttpURLConnection conn) throws Exception {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
}
