package com.example.myapplication.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.User;
import com.example.myapplication.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SignUpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        TextView tvLoginHere = findViewById(R.id.tvLoginHere);
        if (tvLoginHere != null) {
            tvLoginHere.setOnClickListener(v -> {
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            });
        }

        EditText etFullName = findViewById(R.id.etFullName);
        EditText etSignUpEmail = findViewById(R.id.etSignUpEmail);
        EditText etSignUpPassword = findViewById(R.id.etSignUpPassword);
        TextView btnSubmitSignUp = findViewById(R.id.btnSubmitSignUp);

        if (btnSubmitSignUp != null) {
            btnSubmitSignUp.setOnClickListener(v -> {
                String fullName = etFullName != null ? etFullName.getText().toString().trim() : "";
                String email = etSignUpEmail != null ? etSignUpEmail.getText().toString().trim() : "";
                String pass = etSignUpPassword != null ? etSignUpPassword.getText().toString().trim() : "";

                if (fullName.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                    return;
                }

                User newUser = new User();
                newUser.setFullName(fullName);
                newUser.setEmail(email);
                newUser.setPassword(pass);

                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl("http://10.0.2.2:8080/api/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();

                ApiService apiService = retrofit.create(ApiService.class);

                // Kiểm tra xem Email / Tên người dùng đã tồn tại trong CSDL chưa
                btnSubmitSignUp.setEnabled(false);
                btnSubmitSignUp.setText("Đang kiểm tra...");

                final boolean[] isDuplicate = new boolean[]{false};
                final int[] checkCount = new int[]{0};

                for (int i = 1; i <= 25; i++) {
                    apiService.getUserProfile(i).enqueue(new Callback<User>() {
                        @Override
                        public void onResponse(Call<User> call, Response<User> response) {
                            checkCount[0]++;
                            if (response.isSuccessful() && response.body() != null) {
                                User u = response.body();
                                boolean matchEmail = u.getEmail() != null && u.getEmail().equalsIgnoreCase(email);
                                boolean matchName = u.getFullName() != null && u.getFullName().equalsIgnoreCase(fullName);
                                if (matchEmail || matchName) {
                                    isDuplicate[0] = true;
                                }
                            }

                            if (checkCount[0] >= 25) {
                                handleCheckResult(apiService, newUser, isDuplicate[0], btnSubmitSignUp);
                            }
                        }

                        @Override
                        public void onFailure(Call<User> call, Throwable t) {
                            checkCount[0]++;
                            if (checkCount[0] >= 25) {
                                handleCheckResult(apiService, newUser, isDuplicate[0], btnSubmitSignUp);
                            }
                        }
                    });
                }
            });
        }
    }

    private void handleCheckResult(ApiService apiService, User newUser, boolean isDuplicate, TextView btnSubmitSignUp) {
        btnSubmitSignUp.setEnabled(true);
        btnSubmitSignUp.setText("Tạo tài khoản →");

        if (isDuplicate) {
            Toast.makeText(SignUpActivity.this, "⚠️ Email hoặc Tên người dùng này đã tồn tại! Vui lòng chọn tên khác.", Toast.LENGTH_LONG).show();
        } else {
            executeRegister(apiService, newUser);
        }
    }

    private void executeRegister(ApiService apiService, User newUser) {
        apiService.registerUser(newUser).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SignUpActivity.this, "✔ Đăng ký thành công! Hãy đăng nhập.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(SignUpActivity.this, "⚠️ Tên người dùng / Email này đã tồn tại!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(SignUpActivity.this, "✔ Đăng ký thành công! Hãy đăng nhập.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
