package com.eagle.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ListUsersClient {

    /** Call /listUsers on the server; optional Bearer token. */
    public static String listUsers() throws Exception {
        return listUsers("http://localhost:8080", null);
    }

    public static String listUsers(String baseUrl, String bearerToken) throws Exception {
        URL url = new URL(baseUrl + "/listUsers");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (bearerToken != null && !bearerToken.isBlank()) {
            conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
        }

        int code = conn.getResponseCode();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            for (String line; (line = br.readLine()) != null; ) sb.append(line);
            return sb.toString(); // JSON (UserListResponse)
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("User List Response: " + listUsers());
    }
}
