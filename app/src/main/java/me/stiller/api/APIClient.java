package me.stiller.api;

import java.util.concurrent.TimeUnit;

import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class APIClient {

    private static OkHttpClient getClient() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(5, 20, TimeUnit.SECONDS))
                .addInterceptor(interceptor)
                .build();
    }

    private static OkHttpClient getGithubClient(String token) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(5, 20, TimeUnit.SECONDS))
                .addInterceptor(interceptor)
                .addInterceptor(chain -> {
                    Request originalRequest = chain.request();
                    Request.Builder builder = originalRequest.newBuilder()
                            .header("Authorization", "Bearer " + token);
                    Request newRequest = builder.build();
                    return chain.proceed(newRequest);
                })
                .build();
    }

    public static Retrofit getGoogleServer() {
        return new Retrofit.Builder()
                .baseUrl("https://www.googleapis.com")
                .addConverterFactory(JacksonConverterFactory.create())
                .client(getClient())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
    }

    public static Retrofit getFacebookServer() {
        return new Retrofit.Builder()
                .baseUrl("https://graph.facebook.com")
                .addConverterFactory(JacksonConverterFactory.create())
                .client(getClient())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
    }

    public static Retrofit getGithubServer() {
        return new Retrofit.Builder()
                .baseUrl("https://github.com")
                .client(getClient())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
    }

    public static Retrofit getGithubApiServer(String token) {
        return new Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(JacksonConverterFactory.create())
                .client(getGithubClient(token))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
    }

}