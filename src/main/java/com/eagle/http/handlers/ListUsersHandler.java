package com.eagle.http.handlers;

import com.eagle.dao.UserDao;
import com.eagle.model.response.ErrorResponse;
import com.eagle.model.response.UserListResponse;
import com.sun.net.httpserver.HttpExchange;

import java.util.List;

/** Example handler using BaseHandler utilities. */
public class ListUsersHandler extends BaseHandler {
    private final UserDao userDao = new UserDao();

    @Override
    protected void doHandle(HttpExchange ex) throws Exception {
        if (!ensureMethod(ex, "GET", "OPTIONS")) return; // writes 405 when not allowed
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.getResponseHeaders().set("Allow", "GET, OPTIONS");
            ex.sendResponseHeaders(204, -1);
            return;
        }

        try {
            List<String> users = userDao.getAllUsers();
            UserListResponse resp = new UserListResponse("users list", users);
            writeJson(ex, 200, resp);
        } catch (Exception e) {
            writeJson(ex, 500, new ErrorResponse("internal_error", e.getMessage()));
        }
    }
}
