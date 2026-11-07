package com.example.myapplication.network;

import com.example.myapplication.model.Project;
import com.example.myapplication.model.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitClient {

    private static final String DEFAULT_BASE_URL = "http://10.0.2.2:8080/api/";
    private static volatile Retrofit retrofitInstance;
    private static volatile ApiService apiServiceInstance;

    // Bộ nhớ đệm RAM (In-Memory Cache) giúp chuyển màn hình mượt tức thì 0ms
    private static volatile List<Task> cachedTasks = Collections.emptyList();
    private static volatile List<Project> cachedProjects = Collections.emptyList();
    private static volatile long lastTasksFetchTimeMs = 0L;
    private static volatile long lastProjectsFetchTimeMs = 0L;

    private RetrofitClient() {
    }

    public static String getBaseUrl() {
        return DEFAULT_BASE_URL;
    }

    public static Retrofit getRetrofit() {
        if (retrofitInstance == null) {
            synchronized (RetrofitClient.class) {
                if (retrofitInstance == null) {
                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                            .connectTimeout(5, TimeUnit.SECONDS)
                            .readTimeout(8, TimeUnit.SECONDS)
                            .writeTimeout(8, TimeUnit.SECONDS)
                            .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
                            .retryOnConnectionFailure(true)
                            .build();

                    retrofitInstance = new Retrofit.Builder()
                            .baseUrl(DEFAULT_BASE_URL)
                            .client(okHttpClient)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }
        return retrofitInstance;
    }

    public static ApiService getApiService() {
        if (apiServiceInstance == null) {
            synchronized (RetrofitClient.class) {
                if (apiServiceInstance == null) {
                    apiServiceInstance = getRetrofit().create(ApiService.class);
                }
            }
        }
        return apiServiceInstance;
    }

    public static List<Task> getCachedTasks() {
        return cachedTasks;
    }

    public static void setCachedTasks(List<Task> tasks) {
        if (tasks != null) {
            cachedTasks = new ArrayList<>(tasks);
            lastTasksFetchTimeMs = System.currentTimeMillis();
        }
    }

    public static boolean isTasksCacheFresh(long maxAgeMs) {
        return !cachedTasks.isEmpty() && (System.currentTimeMillis() - lastTasksFetchTimeMs) < maxAgeMs;
    }

    public static List<Project> getCachedProjects() {
        return cachedProjects;
    }

    public static void setCachedProjects(List<Project> projects) {
        if (projects != null) {
            cachedProjects = new ArrayList<>(projects);
            lastProjectsFetchTimeMs = System.currentTimeMillis();
        }
    }

    public static boolean isProjectsCacheFresh(long maxAgeMs) {
        return !cachedProjects.isEmpty() && (System.currentTimeMillis() - lastProjectsFetchTimeMs) < maxAgeMs;
    }

    public static void invalidateCache() {
        lastTasksFetchTimeMs = 0L;
        lastProjectsFetchTimeMs = 0L;
    }
}
