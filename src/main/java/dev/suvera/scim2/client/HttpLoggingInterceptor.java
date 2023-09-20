package dev.suvera.scim2.client;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import org.jboss.logging.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * author: suvera
 * date: 10/7/2020 12:54 PM
 */
public class HttpLoggingInterceptor implements Interceptor {

    private final String purpose;
    private static final Logger log = Logger.getLogger(HttpLoggingInterceptor.class);

    public HttpLoggingInterceptor(String purpose) {
        this.purpose = purpose;
    }

    @NotNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        long t1 = System.nanoTime();
        log.debugf("OkHttp [%s] Sending %s request %s on %s%n\n%s\nRequestBody:%s", purpose, request.method(), request.url(), chain.connection(), request.headers(), request.body());

        Response response = chain.proceed(request);

        long t2 = System.nanoTime();
        log.debugf("OkHttp [%s] Received response for %s in %.1fms%n\n%s\nResponseBody:%s", purpose, response.request().url(), (t2 - t1) / 1e6d, response.headers(), response.body());

        return response;
    }
}
