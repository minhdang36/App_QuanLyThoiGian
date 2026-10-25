package com.example.myapplication.network;

import com.example.myapplication.model.AiRequest;
import com.example.myapplication.model.Project;
import com.example.myapplication.model.Task;
import com.example.myapplication.model.User;

import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {
    @GET("projects")
    Call<List<Project>> getProjects();

    @GET("tasks")
    Call<List<Task>> getTasks();

    @POST("tasks")
    Call<Task> addTask(@Body Task task);

    @GET("tasks")
    Call<List<Task>> getAllTasks();

    @DELETE("tasks/{id}")
    Call<Void> deleteTask(@Path("id") int id);

    @PUT("tasks/{id}")
    Call<Task> updateTask(@Path("id") int id, @Body Task task);

    @POST("projects")
    Call<Project> addProject(@Body Project project);

    @PUT("projects/{id}")
    Call<Project> updateProject(@Path("id") int id, @Body Project project);

    @DELETE("projects/{id}")
    Call<Void> deleteProject(@Path("id") int id);

    @GET("users/{id}")
    Call<User> getUserProfile(@Path("id") int id);

    @POST("users/register")
    Call<User> registerUser(@Body User user);

    @POST("users/login")
    Call<User> loginUser(@Body User user);

    @GET("projects/user/{userId}")
    Call<List<Project>> getProjectsByUser(@Path("userId") int userId);

    @GET("tasks/user/{userId}")
    Call<List<Task>> getTasksByUser(@Path("userId") int userId);

    @POST("ai/ask")
    Call<ResponseBody> askAiBackend(@Body AiRequest request);
}
