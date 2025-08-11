package com.eagle.client;

import java.net.HttpURLConnection;

public class LoginClient extends AbsClient {
    public static String login(String username, String password) throws Exception {
        return new LoginClient().login(DEFAULT_BASE_URL, username, password);
    }
    public String login(String baseUrl, String username, String password) throws Exception {
        HttpURLConnection c = open(joinUrl(baseUrl, "/login"), "POST");
        enableBody(c); setJsonHeaders(c);
        String json = "{\"username\":\"" + escape(username) + "\",\"password\":\"" + escape(password) + "\"}";
        writeJson(c, json);
        int code = c.getResponseCode();
        return readBody(c, code);
    }
    private static String escape(String s){return s==null?"":s.replace("\\","\\\\").replace("\"","\\\"");}
}