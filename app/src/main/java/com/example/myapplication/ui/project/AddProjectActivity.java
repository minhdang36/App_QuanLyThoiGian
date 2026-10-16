package com.example.myapplication.ui.project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.Project;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.ui.main.MainActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AddProjectActivity extends AppCompatActivity {

    private EditText etProjectName, etProjectDesc;
    private TextView btnSaveProject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_project);

        etProjectName = findViewById(R.id.etProjectName);
        etProjectDesc = findViewById(R.id.etProjectDesc);
        btnSaveProject = findViewById(R.id.btnSaveProject);

        if (btnSaveProject != null) {
            btnSaveProject.setOnClickListener(v -> saveProjectToDatabase());
        }
    }

    private void saveProjectToDatabase() {
        String name = etProjectName != null ? etProjectName.getText().toString().trim() : "";
        String desc = etProjectDesc != null ? etProjectDesc.getText().toString().trim() : "";

        if (name.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên danh mục/dự án!", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", -1);

        if (currentUserId == -1) {
            Toast.makeText(this, "Lỗi: Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        Project newProject = new Project();
        newProject.setName(name);
        newProject.setDescription(desc);
        newProject.setUserId(currentUserId);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        apiService.addProject(newProject).enqueue(new Callback<Project>() {
            @Override
            public void onResponse(Call<Project> call, Response<Project> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddProjectActivity.this, "Đã thêm danh mục mới!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(AddProjectActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(AddProjectActivity.this, "Lỗi thêm danh mục: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Project> call, Throwable t) {
                Toast.makeText(AddProjectActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
            }
        });
    }
}