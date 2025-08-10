package com.eagle.http;

import com.eagle.service.UserService;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;

public class SignupHandler implements HttpHandler {
    private static final Gson gson = new Gson();
    private final UserService userService = new UserService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            try (Reader reader = new InputStreamReader(exchange.getRequestBody())) {
                SignupRequest signupReq = gson.fromJson(reader, SignupRequest.class);

                boolean created = userService.signup(signupReq.getUsername(), signupReq.getPassword());
                String jsonResponse = gson.toJson(
                        new PingResponse(created ? "Signup successful" : "Signup failed", System.currentTimeMillis())
                );

                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(created ? 200 : 400, jsonResponse.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonResponse.getBytes());
                }
            } catch (Exception e) {
                e.printStackTrace();
                String errorResponse = gson.toJson(new PingResponse("Error: " + e.getMessage(),System.currentTimeMillis()));
                exchange.sendResponseHeaders(500, errorResponse.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(errorResponse.getBytes());
                }
            }
        } else {
            exchange.sendResponseHeaders(405, -1); // Method not allowed
        }
    }
}
