package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.model.Project;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AddProjectActivity extends AppCompatActivity {

    private int projectId = -1; // -1 nghĩa là Đang thêm mới

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_project);

        EditText etName = findViewById(R.id.etProjectName);
        EditText etDesc = findViewById(R.id.etProjectDesc);
        TextView btnSave = findViewById(R.id.btnSaveProject);

        // KIỂM TRA XEM ĐÂY LÀ CHẾ ĐỘ THÊM HAY SỬA
        projectId = getIntent().getIntExtra("PROJECT_ID", -1);
        if (projectId != -1) {
            etName.setText(getIntent().getStringExtra("PROJECT_NAME"));
            etDesc.setText(getIntent().getStringExtra("PROJECT_DESC"));
            btnSave.setText("CẬP NHẬT DANH MỤC");

            TextView tvHeader = findViewById(R.id.tvHeaderTitle);
            if (tvHeader != null) {
                tvHeader.setText("Sửa Danh Mục");
            }
        }

        btnSave.setOnClickListener(v -> {
            // Lấy dữ liệu từ giao diện (sửa lại đúng tên biến etName, etDesc)
            String name = etName.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên danh mục!", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. "MÓC TÚI" LẤY ID NGƯỜI DÙNG ĐANG ĐĂNG NHẬP
            SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
            int currentUserId = sharedPref.getInt("USER_ID", -1);

            if (currentUserId == -1) {
                Toast.makeText(this, "Lỗi: Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. KHỞI TẠO DỰ ÁN VÀ GÁN DỮ LIỆU
            Project projectData = new Project();
            projectData.setName(name);
            projectData.setDescription(desc);
            projectData.setColorHex("#FF9800");

            // DÒNG QUAN TRỌNG NHẤT: Gắn chủ sở hữu cho dự án này
            projectData.setUserId(currentUserId);

            // Khởi tạo apiService
            ApiService apiService = getRetrofitClient().create(ApiService.class);

            // 3. XỬ LÝ PHÂN NHÁNH: SỬA HOẶC THÊM MỚI
            if (projectId != -1) {
                // NẾU LÀ SỬA (Cập nhật)
                apiService.updateProject(projectId, projectData).enqueue(new Callback<Project>() {
                    @Override
                    public void onResponse(Call<Project> call, Response<Project> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(AddProjectActivity.this, "Đã cập nhật!", Toast.LENGTH_SHORT).show();
                            finish(); // Đóng trang này
                        }
                    }
                    @Override
                    public void onFailure(Call<Project> call, Throwable t) {
                        Toast.makeText(AddProjectActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // NẾU LÀ THÊM MỚI
                apiService.addProject(projectData).enqueue(new Callback<Project>() {
                    @Override
                    public void onResponse(Call<Project> call, Response<Project> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(AddProjectActivity.this, "Đã thêm mới!", Toast.LENGTH_SHORT).show();
                            finish(); // Đóng trang này
                        }
                    }
                    @Override
                    public void onFailure(Call<Project> call, Throwable t) {
                        Toast.makeText(AddProjectActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private Retrofit getRetrofitClient() {
        return new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}