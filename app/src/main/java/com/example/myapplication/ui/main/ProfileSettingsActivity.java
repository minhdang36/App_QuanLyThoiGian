package com.example.myapplication.ui.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.User;
import com.example.myapplication.network.ApiService;
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

        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserBadge = findViewById(R.id.tvUserBadge);
        tvStreakDays = findViewById(R.id.tvStreakDays);
        tvFocusHours = findViewById(R.id.tvFocusHours);

        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", 1);
        String savedEmail = sharedPref.getString("USER_EMAIL", "");
        String savedName = sharedPref.getString("USER_NAME", "");

        if (!savedName.isEmpty() && tvUserName != null) {
            tvUserName.setText(savedName);
        }
        if (!savedEmail.isEmpty() && tvUserEmail != null) {
            tvUserEmail.setText(savedEmail);
        }

        loadUserDataFromApi(currentUserId);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

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

        View btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.clear();
                editor.apply();

                Toast.makeText(this, "Đã đăng xuất tài khoản", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

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

                    if (tvUserName != null && user.getFullName() != null && !user.getFullName().isEmpty()) {
                        tvUserName.setText(user.getFullName());
                    }
                    if (tvUserEmail != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
                        tvUserEmail.setText(user.getEmail());
                    }
                    if (tvUserBadge != null && user.getMemberLevel() != null) {
                        tvUserBadge.setText(user.getMemberLevel());
                    }
                    if (tvStreakDays != null) {
                        tvStreakDays.setText(user.getStreakDays() + " Ngày");
                    }
                    if (tvFocusHours != null) {
                        tvFocusHours.setText(user.getFocusHours() + " Giờ");
                    }
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
            }
        });
    }
}