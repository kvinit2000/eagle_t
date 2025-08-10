package com.eagle.http;

import com.eagle.model.response.PingResponse;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class BasicHttpServer {

    public static void main(String[] args) throws IOException {
        // Create an HTTP server listening on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Create a context for the "/hello" path
        server.createContext("/hello", new HelloHandler());
        // Create a context for the "/signup" path
        server.createContext("/signup", new SignupHandler());
        server.createContext("/listUsers", new ListUsersHandler());


        // Thread pool for handling requests
        server.setExecutor(Executors.newFixedThreadPool(10));
        System.out.println("Server started on port 8080");
        server.start();
    }

    // Define a handler for the /hello path
    static class HelloHandler implements HttpHandler {
        private static final Gson gson = new Gson();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            PingResponse pingResponse = new PingResponse("Hello from Basic HTTP Server!", System.currentTimeMillis());
            String jsonResponse = gson.toJson(pingResponse);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(jsonResponse.getBytes());
            }
        }
    }
}
