package com.eagle.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class PingClient {
    public static void main(String[] args) {
        try {
            URL url = new URL("http://localhost:8080/hello");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Read the response
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            // Print the JSON response
            System.out.println("Server Response: " + response.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
