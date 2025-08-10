package com.eagle.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LoginClient {

    /** Call /login with given credentials and return raw JSON (LoginResponse). */
    public static String login(String username, String password) throws Exception {
        URL url = new URL("http://localhost:8080/login");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        String jsonBody = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            for (String line; (line = br.readLine()) != null; ) sb.append(line);
            return sb.toString();
        }
    }

    /** Standalone run for manual testing. */
    public static void main(String[] args) throws Exception {
        System.out.println("Login Response: " + login("testUser", "pass123"));
    }
}
