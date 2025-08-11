package com.eagle.util;

import com.google.gson.Gson;

/** Central place for JSON (keeps one Gson instance + helpers). */
public final class Jsons {
    private static final Gson GSON = new Gson();

    private Jsons() {}

    public static String toJson(Object o) {
        return GSON.toJson(o);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        return GSON.fromJson(json, type);
    }
}
