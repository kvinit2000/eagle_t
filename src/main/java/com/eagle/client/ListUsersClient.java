package com.eagle.client;

import java.net.HttpURLConnection;

public class ListUsersClient extends AbsClient {
    public static String listUsers() throws Exception { return new ListUsersClient().listUsers(DEFAULT_BASE_URL, null); }
    public String listUsers(String baseUrl, String bearerToken) throws Exception {
        HttpURLConnection c = open(joinUrl(baseUrl, "/listUsers"), "GET");
        c.setRequestProperty("Accept", "application/json");
        setAuth(c, bearerToken);
        int code = c.getResponseCode();
        String body = readBody(c, code);
        return "HTTP " + code + (body.isBlank()?"":" -> "+body);
    }
}