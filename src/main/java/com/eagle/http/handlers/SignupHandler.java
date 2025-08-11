package com.eagle.http.handlers;

import com.eagle.dao.UserDao;
import com.eagle.model.request.SignupRequest;
import com.eagle.model.response.ErrorResponse;
import com.eagle.model.response.SignupResponse;
import com.sun.net.httpserver.HttpExchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;

/**
 * Refactored to use BaseHandler utilities.
 */
public class SignupHandler extends BaseHandler {
    private static final Logger log = LogManager.getLogger(SignupHandler.class);
    private final UserDao userDao = new UserDao();

    @Override
    protected void doHandle(HttpExchange ex) throws Exception {
        if (!ensureMethod(ex, "POST", "OPTIONS")) return; // writes 405 + Allow when wrong
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.getResponseHeaders().set("Allow", "POST, OPTIONS");
            ex.sendResponseHeaders(204, -1);
            return;
        }

        if (!isJsonRequest(ex)) {
            writeJson(ex, 415, new ErrorResponse("unsupported_media_type", "Content-Type must be application/json"));
            return;
        }

        SignupRequest req;
        try {
            req = readJson(ex, SignupRequest.class);
        } catch (Exception parseErr) {
            writeJson(ex, 400, new ErrorResponse("bad_request", "Malformed JSON body"));
            return;
        }

        if (req == null || isBlank(req.getUsername()) || isBlank(req.getPassword())) {
            writeJson(ex, 400, new ErrorResponse("bad_request", "username and password are required"));
            return;
        }

        // Optional dob parsing (yyyy-MM-dd)
        LocalDate dob = null;
        if (!isBlank(req.getDob())) {
            try {
                dob = LocalDate.parse(req.getDob());
            } catch (Exception pe) {
                log.warn("Invalid dob format received: {}", req.getDob());
                // Keep dob as null; we could also 400 here if strict
            }
        }

        boolean ok;
        try {
            ok = userDao.saveUser(
                    req.getUsername(),
                    req.getPassword(),
                    nullIfBlank(req.getEmail()),
                    dob,
                    nullIfBlank(req.getAddress()),
                    nullIfBlank(req.getPin()),
                    nullIfBlank(req.getPhone())
            );
        } catch (Exception dbErr) {
            log.error("Signup DB error", dbErr);
            writeJson(ex, 500, new ErrorResponse("internal_error", "Persistence error"));
            return;
        }

        if (!ok) {
            writeJson(ex, 409, new ErrorResponse("conflict", "Username already exists or could not be saved"));
            return;
        }

        // Success: fetch bearer token generated on save
        String authToken = null;
        try {
            authToken = userDao.getAuthToken(req.getUsername());
        } catch (Exception tokenErr) {
            log.warn("Auth token lookup failed after signup for {}", req.getUsername(), tokenErr);
        }

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
        writeJson(ex, 201, resp);
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String nullIfBlank(String s) { return isBlank(s) ? null : s.trim(); }
}
