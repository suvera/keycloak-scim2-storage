package dev.suvera.scim2.client;

import dev.suvera.scim2.schema.ex.ScimException;
import okhttp3.*;
import okhttp3.FormBody.Builder;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * author: suvera
 * date: 10/17/2020 2:26 PM
 */
@SuppressWarnings({"RedundantThrows", "FieldCanBeLocal", "unused"})
public class Scim2ClientBuilder {
    private final String endPoint;
    private final OkHttpClient.Builder builder;
    private static final boolean DEBUG = true;
    private boolean debugEnabled = false;
    private String spConfigJson;
    private String resourceTypesJson;
    private String schemasJson;

    public Scim2ClientBuilder(String endPoint) {
        this.endPoint = endPoint;
        builder = new OkHttpClient.Builder()
                .connectTimeout(1200000, TimeUnit.SECONDS)
                .readTimeout(1200000, TimeUnit.SECONDS);

        if (DEBUG) {
            builder.networkInterceptors().add(new HttpLoggingInterceptor("NETWORK"));
            builder.interceptors().add(new HttpLoggingInterceptor("APP"));
            debugEnabled = true;
        }
    }

    public Scim2Client build() throws ScimException {
        return new Scim2ClientImpl(endPoint, builder.build(), spConfigJson, resourceTypesJson, schemasJson);
    }

    public Scim2ClientBuilder enableDebugging() {
        if (!debugEnabled) {
            builder.networkInterceptors().add(new HttpLoggingInterceptor("NETWORK"));
            builder.interceptors().add(new HttpLoggingInterceptor("APP"));
            debugEnabled = true;
        }
        return this;
    }

    public Scim2ClientBuilder usernamePassword(String username, String password) {
        builder.authenticator(new BasicAuthenticator(username, password));
        return this;
    }

    public Scim2ClientBuilder bearerToken(String token) {
        builder.authenticator(new BearerAuthenticator(token));
        return this;
    }

    public Scim2ClientBuilder clientSecret(String authorityUrl, String username, String password, String clientId, String clientSecret) {
        String tokenKey = "tokenKey";

        LoadingCache<String, Token> tokenCache = CacheBuilder.newBuilder().build(
            new CacheLoader<String, Token>() {

                @Override
                public Token load(String key) throws Exception {
                    if (key.equals(tokenKey)) {
                        OkHttpClient client = new OkHttpClient();

                        Builder builder = new FormBody.Builder();

                        if (clientId != null) {
                            builder.add("client_id", clientId);

                            if (clientSecret != null) {
                                builder.add("client_secret", clientSecret);
                            }

                            if (username != null && password != null) {
                                builder
                                    .add("username", username)
                                    .add("password", password)
                                    .add("grant_type", "password");
                            } else {
                                builder.add("grant_type", "client_credentials");
                            }
                        }
                        
                        RequestBody body = builder.build();
    
                        Request tokenRequest = new Request.Builder()
                        .url(StringUtils.strip(authorityUrl, "/") + "/protocol/openid-connect/token")
                        .post(body)
                        .build();
                        
                        Response tokenResponse = client.newCall(tokenRequest).execute();
                        if (!tokenResponse.isSuccessful()) {
                            return null;
                        }
    
                        ObjectMapper mapper = new ObjectMapper();
                        return mapper.readValue(tokenResponse.body().string(), Token.class);
                    }
                    return null;
                }
            }
        );

        builder.addInterceptor(new Interceptor() {

            @Override
            public Response intercept(Chain chain) throws IOException {
                Request authenticatedRequest = tokenRequest(tokenKey, tokenCache, chain.request());

                if (authenticatedRequest != null) {
                    return chain.proceed(authenticatedRequest);
                }
                return chain.proceed(chain.request());
            }

        });
        
        builder.authenticator(new Authenticator() {

            @Override
            @javax.annotation.Nullable
            public Request authenticate(@javax.annotation.Nullable Route route, Response response) throws IOException {
                tokenCache.invalidate(tokenKey);

                return tokenRequest(tokenKey, tokenCache, response.request());
            }
            
        });
        return this;
    }

    private Request tokenRequest(String tokenKey, LoadingCache<String, Token> tokenCache, Request request) {
        Token token = null;
        try {
            token = tokenCache.get(tokenKey);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        if (token != null) {
            return request.newBuilder()
            .header("Authorization", "Bearer " + token.getAccessToken())
            .build();
        }
        return null;
    }

    public Scim2ClientBuilder serviceProviderConfig(String spConfigJson) {
        this.spConfigJson = spConfigJson;
        return this;
    }

    public Scim2ClientBuilder resourceTypes(String resourceTypesJson) {
        this.resourceTypesJson = resourceTypesJson;
        return this;
    }

    public Scim2ClientBuilder schemas(String schemasJson) {
        this.schemasJson = schemasJson;
        return this;
    }

    public Scim2ClientBuilder allowSelfSigned(boolean flag) {
        if (!flag) {
            return this;
        }

        try {
            TrustManager trustAll = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[]{};
                }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManager[] trustManagers = new TrustManager[]{trustAll};

            sslContext.init(null, trustManagers, new SecureRandom());
            builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustManagers[0]);
            builder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return this;
    }

    @SuppressWarnings("RedundantThrows")
    private static class BasicAuthenticator implements Authenticator {
        private final String username;
        private final String password;

        public BasicAuthenticator(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Nullable
        @Override
        public Request authenticate(@Nullable Route route, @NotNull Response response) throws IOException {
            String credential = Credentials.basic(username, password);
            return response.request()
                    .newBuilder()
                    .header("Authorization", credential).build();
        }
    }

    @SuppressWarnings("RedundantThrows")
    private static class BearerAuthenticator implements Authenticator {
        private final String token;

        public BearerAuthenticator(String token) {
            this.token = token;
        }

        @Nullable
        @Override
        public Request authenticate(@Nullable Route route, @NotNull Response response) throws IOException {
            return response.request().newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .build();
        }
    }
}

