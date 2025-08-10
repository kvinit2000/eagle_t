package com.eagle.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class SignupClient {

    /** Call /signup with a random username (handy for loops / MasterClient). */
    public static String signupRandom() throws Exception {
        String username = "user_" + UUID.randomUUID().toString().substring(0, 6);
        return signup(username, "pass123");
    }

    /** Call /signup with explicit credentials. */
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
        BufferedReader br = new BufferedReader(new InputStreamReader(
                (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8
        ));

        StringBuilder sb = new StringBuilder();
        for (String line; (line = br.readLine()) != null; ) sb.append(line);
        br.close();
        return sb.toString(); // raw JSON (SignupResponse)
    }

    /** Keep runnable standalone for quick manual testing. */
    public static void main(String[] args) throws Exception {
        System.out.println("Signup Response: " + signupRandom());
    }
}
