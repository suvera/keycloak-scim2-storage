package dev.suvera.scim2.schema.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * author: suvera
 * date: 10/17/2020 2:48 PM
 */
public class UrlUtil {

    public static String urlEncode(Object str) {
        try {
            return URLEncoder.encode(str.toString(), StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return str.toString();
        }
    }

    public static String queryString(Object params) {
        if (params == null) {
            return null;
        } else if (params instanceof Map) {
            return ((Map<?, ?>) params).entrySet().stream()
                    .map(p -> urlEncode(p.getKey()) + "=" + urlEncode(p.getValue()))
                    .reduce((p1, p2) -> p1 + "&" + p2)
                    .orElse("");
        } else {
            return params.toString();
        }
    }
}
