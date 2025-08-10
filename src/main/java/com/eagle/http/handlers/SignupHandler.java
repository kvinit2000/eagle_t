package com.eagle.http.handlers;

import com.eagle.dao.UserDao;
import com.eagle.model.request.SignupRequest;
import com.eagle.model.response.SignupResponse;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public class SignupHandler implements HttpHandler {
    private static final Logger log = LogManager.getLogger(SignupHandler.class);
    private static final Gson GSON = new Gson();
    private final UserDao userDao = new UserDao();

    @Override
    public void handle(HttpExchange exchange) {
        try {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            SignupRequest req = GSON.fromJson(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8),
                    SignupRequest.class
            );

            if (req == null || isBlank(req.getUsername()) || isBlank(req.getPassword())) {
                sendJson(exchange, 400, new SignupResponse(
                        false,
                        "username and password are required",
                        null, null, null, null, null, null
                ));
                return;
            }

            // Try parse dob if present (optional)
            LocalDate dob = null;
            if (!isBlank(req.getDob())) {
                try {
                    dob = LocalDate.parse(req.getDob()); // expects yyyy-MM-dd
                } catch (Exception e) {
                    log.warn("Invalid dob format received: {}", req.getDob());
                }
            }

            boolean ok = userDao.saveUser(
                    req.getUsername(),
                    req.getPassword(),
                    nullIfBlank(req.getEmail()),
                    dob,
                    nullIfBlank(req.getAddress()),
                    nullIfBlank(req.getPin()),
                    nullIfBlank(req.getPhone())
            );

            if (ok) {
                String dobOut = req.getDob();
                sendJson(exchange, 201, new SignupResponse(
                        true,
                        "Signup successful",
                        req.getUsername(),
                        nullIfBlank(req.getEmail()),
                        dobOut,
                        nullIfBlank(req.getAddress()),
                        nullIfBlank(req.getPin()),
                        nullIfBlank(req.getPhone())
                ));
            } else {
                String dobOut = req.getDob();
                sendJson(exchange, 409, new SignupResponse(
                        false,
                        "Username already exists or could not be saved",
                        req.getUsername(),
                        nullIfBlank(req.getEmail()),
                        dobOut,
                        nullIfBlank(req.getAddress()),
                        nullIfBlank(req.getPin()),
                        nullIfBlank(req.getPhone())
                ));
            }

        } catch (Exception e) {
            log.error("Signup error", e);
            try {
                sendJson(exchange, 500, new SignupResponse(
                        false,
                        "Internal error",
                        null, null, null, null, null, null
                ));
            } catch (Exception ignored) {}
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String nullIfBlank(String s) {
        return isBlank(s) ? null : s.trim();
    }

    private void sendJson(HttpExchange ex, int status, Object body) throws Exception {
        byte[] out = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, out.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(out); }
    }
}
