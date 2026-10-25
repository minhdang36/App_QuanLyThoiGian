package com.example.myapplication.network;

import com.example.myapplication.model.AiRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AiRepository {

    public interface AiCallback {
        void onSuccess(String responseText);
        void onError(String errorMessage);
    }

    private static final String DEFAULT_BASE_URL = "http://10.0.2.2:8080/api/";
    private final ApiService apiService;

    public AiRepository() {
        this(DEFAULT_BASE_URL);
    }

    public AiRepository(String baseUrl) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        this.apiService = retrofit.create(ApiService.class);
    }

    public void sendPromptToBackend(String prompt, AiCallback callback) {
        AiRequest request = new AiRequest(prompt);
        apiService.askAiBackend(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String resultText = response.body().string();
                        callback.onSuccess(resultText);
                    } catch (Exception e) {
                        callback.onError("Lỗi đọc dữ liệu từ Server: " + e.getMessage());
                    }
                } else {
                    callback.onError("Lỗi Server Spring Boot (Mã " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onError("Không thể kết nối tới Server Spring Boot: " + t.getMessage());
            }
        });
    }
}
