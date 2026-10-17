package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.model.User;
import com.example.myapplication.ui.auth.LoginActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProfileSettingsActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserEmail, tvUserBadge, tvStreakDays, tvFocusHours;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        // 1. Ánh xạ dữ liệu
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserBadge = findViewById(R.id.tvUserBadge);
        tvStreakDays = findViewById(R.id.tvStreakDays);
        tvFocusHours = findViewById(R.id.tvFocusHours);

        // 2. Tải dữ liệu Profile của đúng người đang đăng nhập
        loadUserProfile();

        // 3. Nút Back quay lại
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // 4. Chức năng Chế độ tối
        Switch switchDarkMode = findViewById(R.id.switchDarkMode);
        if (switchDarkMode != null) {
            switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    Toast.makeText(this, "Đã bật chế độ tối!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Đã tắt chế độ tối!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // 5. Nút Đăng xuất
        android.view.View btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                // Xóa session
                android.content.SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
                android.content.SharedPreferences.Editor editor = sharedPref.edit();
                editor.clear();
                editor.apply();

                Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();

                // Quay về Login
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    // Hàm gọi API lấy thông tin User
    private void loadUserProfile() {
        // Lấy ID người dùng từ SharedPreferences
        android.content.SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", -1);

        if (currentUserId == -1) {
            Toast.makeText(this, "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiService apiService = retrofit.create(ApiService.class);

        apiService.getUserProfile(currentUserId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User myProfile = response.body();

                    // Đổ dữ liệu thật vào giao diện (Đã sửa lỗi ẩn code)
                    // Đổ dữ liệu thật vào giao diện
                    if (tvUserName != null && myProfile.getFullName() != null) {
                        tvUserName.setText(myProfile.getFullName());
                    }
                    if (tvUserEmail != null && myProfile.getEmail() != null) {
                        tvUserEmail.setText(myProfile.getEmail());
                    }
                    if (tvUserBadge != null && myProfile.getMemberLevel() != null) {
                        tvUserBadge.setText(myProfile.getMemberLevel());
                    }

// BẬT DỮ LIỆU THẬT CHO CHUỖI BÁM ĐÍCH VÀ TẬP TRUNG
                    if (tvStreakDays != null) {
                        // Nối thêm chữ " Ngày" để giống thiết kế tĩnh ban đầu
                        tvStreakDays.setText(myProfile.getStreakDays() + " Ngày");
                    }
                    if (tvFocusHours != null) {
                        // Nối thêm chữ " Giờ"
                        tvFocusHours.setText(myProfile.getFocusHours() + " Giờ");
                    }
                }
            }
            @Override
            public void onFailure(Call<User> call, Throwable t) {
                // Đã sửa tên Activity chuẩn xác
                Toast.makeText(ProfileSettingsActivity.this, "Lỗi tải thông tin!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}