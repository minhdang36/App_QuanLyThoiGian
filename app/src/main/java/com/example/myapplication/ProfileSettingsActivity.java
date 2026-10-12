package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.model.User;

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

        // 1. Ánh xạ dữ liệu hiển thị (Đảm bảo bạn đã thêm ID vào file XML)
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserBadge = findViewById(R.id.tvUserBadge);
        tvStreakDays = findViewById(R.id.tvStreakDays);
        tvFocusHours = findViewById(R.id.tvFocusHours);

        // Tải dữ liệu thật từ MySQL (Mặc định lấy User số 1)
        loadUserDataFromApi(1);

        // 2. Nút Back quay lại
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // 3. Chức năng Chế độ tối
        Switch switchDarkMode = findViewById(R.id.switchDarkMode);
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(this, "Đã bật chế độ tối!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Đã tắt chế độ tối!", Toast.LENGTH_SHORT).show();
            }
        });

        // Tìm nút Đăng xuất (thay R.id.btnLogout bằng ID thật trong XML của bạn)
        android.view.View btnLogout = findViewById(R.id.btnLogout);

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                // 1. Mở SharedPreferences và xóa sạch dữ liệu phiên đăng nhập
                android.content.SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
                android.content.SharedPreferences.Editor editor = sharedPref.edit();
                editor.clear(); // Xóa USER_ID và mọi dữ liệu tạm khác
                editor.apply();

                // 2. Hiển thị thông báo
                android.widget.Toast.makeText(this, "Đã đăng xuất", android.widget.Toast.LENGTH_SHORT).show();

                // 3. Chuyển người dùng về lại trang Đăng nhập
                android.content.Intent intent = new android.content.Intent(this, LoginActivity.class);

                // CỰC KỲ QUAN TRỌNG: Cờ này giúp xóa sạch lịch sử các trang đã mở.
                // Ngăn chặn việc người dùng bấm nút "Back" trên điện thoại để lọt lại vào bên trong app.
                intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);

                startActivity(intent);
                finish();
            });
        }
    }

    // Hàm gọi API lấy thông tin User
    private void loadUserDataFromApi(int userId) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        apiService.getUserProfile(userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();

                    // Ghi đè dữ liệu thật lên giao diện
                    if(tvUserName != null) tvUserName.setText(user.getFullName());
                    if(tvUserEmail != null) tvUserEmail.setText(user.getEmail());
                    if(tvUserBadge != null) tvUserBadge.setText(user.getMemberLevel());
                    if(tvStreakDays != null) tvStreakDays.setText(user.getStreakDays() + " Ngày");
                    if(tvFocusHours != null) tvFocusHours.setText(user.getFocusHours() + " Giờ");
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(ProfileSettingsActivity.this, "Lỗi kết nối tải dữ liệu cá nhân", Toast.LENGTH_SHORT).show();
            }
        });
    }
}