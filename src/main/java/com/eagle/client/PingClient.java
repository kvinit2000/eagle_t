package com.eagle.client;

import java.net.HttpURLConnection;

public class PingClient extends AbsClient {

    // Static convenience overloads delegate to the instance method
    public static String ping() throws Exception {
        return new PingClient().pingInstance(DEFAULT_BASE_URL);
    }

    public static String ping(String baseUrl) throws Exception {
        return new PingClient().pingInstance(baseUrl);
    }

    // Instance method with the actual logic
    public String pingInstance(String baseUrl) throws Exception {
        HttpURLConnection c = open(joinUrl(baseUrl, "/hello"), "GET");
        c.setRequestProperty("Accept", "application/json");
        int code = c.getResponseCode();
        String body = readBody(c, code);
        return (code >= 200 && code < 300)
                ? body
                : ("HTTP " + code + (body.isBlank() ? "" : " -> " + body));
    }
}
