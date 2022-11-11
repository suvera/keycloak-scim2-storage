package dev.suvera.scim2.schema.util;

import java.util.HashMap;
import java.util.Map;

/**
 * author: suvera
 * date: 9/6/2020 4:02 PM
 */
public class Xmap {
    private final Map<String, Object> map = new HashMap<>();

    public static Xmap q() {
        return new Xmap();
    }

    public Xmap k(String key, Object val) {
        map.put(key, val);
        return this;
    }

    public Map<String, Object> get() {
        return map;
    }


}
