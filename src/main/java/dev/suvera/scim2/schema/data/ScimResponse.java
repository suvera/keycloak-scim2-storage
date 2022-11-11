package dev.suvera.scim2.schema.data;

import dev.suvera.scim2.schema.ex.ScimException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * author: suvera
 * date: 10/18/2020 12:08 PM
 */
@SuppressWarnings("unused")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScimResponse {
    private int code;
    private String body;
    private Map<String, List<String>> headers;

    /**
     * Returns the last value corresponding to the specified field, or null.
     */
    public String header(String name) {
        if (headers != null && headers.containsKey(name) && !headers.get(name).isEmpty()) {
            return headers.get(name).get(headers.get(name).size() - 1);
        }
        return null;
    }

    public List<String> headers(String name) {
        if (headers == null) {
            return null;
        }
        return headers.get(name);
    }

    public static ScimResponse of(Response response) throws ScimException {
        try {
            ResponseBody body = response.body();
            return new ScimResponse(
                    response.code(),
                    (body == null) ? null : body.string(),
                    response.headers().toMultimap()
            );
        } catch (IOException e) {
            throw new ScimException("Http Client error", e);
        }
    }
}
