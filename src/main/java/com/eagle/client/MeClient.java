package com.eagle.client;

import com.eagle.model.response.UserProfileResponse;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MeClient {
    private static final Gson GSON = new Gson();

    public static UserProfileResponse getMe(String authToken) throws Exception {
        URL url = new URL("http://localhost:8080/users/me");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + authToken);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            for (String line; (line = br.readLine()) != null;) {
                sb.append(line);
            }
            return GSON.fromJson(sb.toString(), UserProfileResponse.class);
        }
    }
}
