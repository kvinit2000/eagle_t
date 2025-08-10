package com.eagle.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class PingClient {

    public static String ping() throws Exception {
        URL url = new URL("http://localhost:8080/hello");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString(); // Return raw JSON or parsed object
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Ping Response: " + ping());
    }
}
