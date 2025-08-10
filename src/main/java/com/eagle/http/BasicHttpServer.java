package com.eagle.http;

import com.eagle.http.handlers.HelloHandler;
import com.eagle.http.handlers.ListUsersHandler;
import com.eagle.http.handlers.LoginHandler;
import com.eagle.http.handlers.SignupHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class BasicHttpServer {

    public static void main(String[] args) throws IOException {
        // Create an HTTP server listening on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Create a context for the "/hello" path
        server.createContext("/hello", new HelloHandler());
        server.createContext("/signup", new SignupHandler());
        server.createContext("/listUsers", new ListUsersHandler());
        server.createContext("/login", new LoginHandler());


        // Thread pool for handling requests
        server.setExecutor(Executors.newFixedThreadPool(10));
        System.out.println("Server started on port 8080");
        server.start();
    }
}
