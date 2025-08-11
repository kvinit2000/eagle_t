package com.eagle.http.handlers;

import com.eagle.dao.UserDao;
import com.eagle.model.request.SignupRequest;
import com.eagle.model.response.ErrorResponse;
import com.eagle.model.response.SignupResponse;
import com.eagle.util.HttpIO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;

public class SignupHandler implements HttpHandler {
    private static final Logger log = LogManager.getLogger(SignupHandler.class);
    private final UserDao userDao = new UserDao();

    @Override
    public void handle(HttpExchange ex) {
        try {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                safeWriteJson(ex, 405, new ErrorResponse("method_not_allowed", "Use POST"));
                return;
            }

            // Read and parse request JSON
            String body = HttpIO.readBody(ex);
            SignupRequest req = SignupRequest.fromJson(body);

            if (req == null || isBlank(req.getUsername()) || isBlank(req.getPassword())) {
                safeWriteJson(ex, 400, new ErrorResponse("bad_request", "username and password are required"));
                return;
            }

            // Optional dob parsing (yyyy-MM-dd)
            LocalDate dob = null;
            if (!isBlank(req.getDob())) {
                try {
                    dob = LocalDate.parse(req.getDob());
                } catch (Exception pe) {
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

            if (!ok) {
                // Likely username conflict or DB failure
                safeWriteJson(ex, 409, new ErrorResponse("conflict", "Username already exists or could not be saved"));
                return;
            }

            // Success: fetch bearer token generated on save
            String authToken = userDao.getAuthToken(req.getUsername());
            SignupResponse resp = new SignupResponse(
                    true,
                    "Signup successful",
                    req.getUsername(),
                    nullIfBlank(req.getEmail()),
                    req.getDob(),
                    nullIfBlank(req.getAddress()),
                    nullIfBlank(req.getPin()),
                    nullIfBlank(req.getPhone()),
                    authToken
            );
            safeWriteJson(ex, 201, resp);

        } catch (Exception e) {
            log.error("Signup error", e);
            safeWriteJson(ex, 500, new ErrorResponse("internal_error", e.getMessage()));
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String nullIfBlank(String s) {
        return isBlank(s) ? null : s.trim();
    }

    /** Never throw from writing the response; fall back to status-only if needed. */
    private void safeWriteJson(HttpExchange ex, int status, Object payload) {
        try {
            if (payload instanceof SignupResponse sr) {
                HttpIO.writeJson(ex, status, sr.toJson());
            } else if (payload instanceof ErrorResponse er) {
                HttpIO.writeJson(ex, status, er.toJson());
            } else if (payload instanceof String s) {
                HttpIO.writeJson(ex, status, s);
            } else {
                HttpIO.writeJson(ex, status, String.valueOf(payload));
            }
        } catch (Exception writeErr) {
            log.error("Failed to send JSON response", writeErr);
            try { ex.sendResponseHeaders(status, -1); } catch (Exception ignored) {}
        }
    }
}
