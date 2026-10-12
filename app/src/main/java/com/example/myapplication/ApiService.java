package com.example.myapplication;
import com.example.myapplication.model.Project;
import com.example.myapplication.model.Task;
import com.example.myapplication.model.User;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

// Trong ApiService.java
public interface ApiService {
    @GET("projects") // Chỉ để "projects" thay vì "api/projects"
    Call<List<Project>> getProjects();

    @GET("tasks")
    Call<List<Task>> getTasks();
    @POST("tasks")
    Call<Task> addTask(@Body Task task);
    @GET("tasks") // Đường dẫn API tương ứng trên Spring Boot của bạn
    Call<List<Task>> getAllTasks();
    // Gọi API Xóa công việc
    @DELETE("tasks/{id}")
    Call<Void> deleteTask(@Path("id") int id);

    // Gọi API Cập nhật trạng thái (Dùng PUT hoặc PATCH tùy backend của bạn)
    // Giả sử truyền cả object Task lên để update
    @PUT("tasks/{id}")
    Call<Task> updateTask(@Path("id") int id, @Body Task task);
    // API Thêm danh mục/dự án mới
    @POST("projects")
    Call<Project> addProject(@Body Project project);
    @PUT("projects/{id}")
    Call<Project> updateProject(@Path("id") int id, @Body Project project);

    @DELETE("projects/{id}")
    Call<Void> deleteProject(@Path("id") int id);
    // Lấy thông tin tài khoản
    @GET("users/{id}")
    Call<User> getUserProfile(@Path("id") int id);
    @POST("users/register")
    Call<User> registerUser(@Body User user);
    // Gọi API Đăng nhập
    @POST("users/login")
    Call<User> loginUser(@Body User user);
    // Thay vì gọi @GET("projects") lấy tất cả, giờ ta gọi theo ID người dùng
    @GET("projects/user/{userId}")
    Call<List<Project>> getProjectsByUser(@Path("userId") int userId);
    // 1. API Lấy danh sách Dự án CHỈ CỦA RIÊNG USER ĐÓ

    // 2. API Lấy danh sách Công việc CHỈ CỦA RIÊNG USER ĐÓ (Nếu có)
    @GET("tasks/user/{userId}")
    Call<List<Task>> getTasksByUser(@Path("userId") int userId);

}
