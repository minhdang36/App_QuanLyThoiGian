package com.example.myapplication.ui.project;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.Project;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.ui.ai.AiTaskBreakdownActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddProjectActivity extends AppCompatActivity {

    private TextView tvHeaderTitle;
    private EditText etProjectName, etProjectDesc;
    private Switch switchLargeProject;
    private TextView btnSaveProject;

    private int projectId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_project);

        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        etProjectName = findViewById(R.id.etProjectName);
        etProjectDesc = findViewById(R.id.etProjectDesc);
        switchLargeProject = findViewById(R.id.switchLargeProject);
        btnSaveProject = findViewById(R.id.btnSaveProject);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("PROJECT_ID")) {
            projectId = intent.getIntExtra("PROJECT_ID", -1);
            String existingName = intent.getStringExtra("PROJECT_NAME");
            String existingDesc = intent.getStringExtra("PROJECT_DESC");

            if (projectId != -1) {
                if (tvHeaderTitle != null) {
                    tvHeaderTitle.setText("Cập Nhật Dự Án / Danh Mục");
                }
                if (etProjectName != null && existingName != null) {
                    etProjectName.setText(existingName);
                }
                if (etProjectDesc != null && existingDesc != null) {
                    etProjectDesc.setText(existingDesc);
                }
                if (btnSaveProject != null) {
                    btnSaveProject.setText("✔ LƯU THAY ĐỔI");
                }
                if (switchLargeProject != null && switchLargeProject.getParent() instanceof View) {
                    View parentCard = (View) switchLargeProject.getParent();
                    if (parentCard.getParent() instanceof View) {
                        ((View) parentCard.getParent()).setVisibility(View.GONE);
                    }
                }
            }
        }

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

        Project projectData = new Project();
        projectData.setName(name);
        projectData.setDescription(desc);
        projectData.setUserId(currentUserId);

        ApiService apiService = RetrofitClient.getApiService();
        btnSaveProject.setEnabled(false);

        if (projectId != -1) {
            btnSaveProject.setText("Đang cập nhật...");
            apiService.updateProject(projectId, projectData).enqueue(new Callback<Project>() {
                @Override
                public void onResponse(Call<Project> call, Response<Project> response) {
                    btnSaveProject.setEnabled(true);
                    btnSaveProject.setText("✔ LƯU THAY ĐỔI");
                    if (response.isSuccessful()) {
                        RetrofitClient.invalidateCache();
                        Toast.makeText(AddProjectActivity.this, "✔ Đã cập nhật danh mục/dự án!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(AddProjectActivity.this, "Lỗi cập nhật danh mục: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Project> call, Throwable t) {
                    btnSaveProject.setEnabled(true);
                    btnSaveProject.setText("✔ LƯU THAY ĐỔI");
                    Toast.makeText(AddProjectActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            btnSaveProject.setText("Đang tạo...");
            apiService.addProject(projectData).enqueue(new Callback<Project>() {
                @Override
                public void onResponse(Call<Project> call, Response<Project> response) {
                    btnSaveProject.setEnabled(true);
                    btnSaveProject.setText("✔ TẠO DỰ ÁN LỚN");
                    if (response.isSuccessful()) {
                        RetrofitClient.invalidateCache();
                        Toast.makeText(AddProjectActivity.this, "✔ Đã thêm dự án mới!", Toast.LENGTH_SHORT).show();
                        int createdId = response.body() != null ? response.body().getId() : -1;

                        if (switchLargeProject != null && switchLargeProject.isChecked()) {
                            Intent aiIntent = new Intent(AddProjectActivity.this, AiTaskBreakdownActivity.class);
                            aiIntent.putExtra("MAIN_TASK_TITLE", name);
                            aiIntent.putExtra("MAIN_TASK_GOAL", desc);
                            aiIntent.putExtra("CREATED_PROJECT_ID", createdId);
                            startActivity(aiIntent);
                        }
                        finish();
                    } else {
                        Toast.makeText(AddProjectActivity.this, "Lỗi thêm danh mục: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Project> call, Throwable t) {
                    btnSaveProject.setEnabled(true);
                    btnSaveProject.setText("✔ TẠO DỰ ÁN LỚN");
                    Toast.makeText(AddProjectActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
