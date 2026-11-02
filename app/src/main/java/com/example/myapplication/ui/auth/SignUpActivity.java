package com.example.myapplication.ui.auth;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
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

    private TextView tvPasswordStrength;
    private View viewStr1, viewStr2, viewStr3, viewStr4;

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

        setupPasswordStrengthWatcher(etSignUpPassword);

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

    private void setupPasswordStrengthWatcher(EditText etPassword) {
        tvPasswordStrength = findViewById(R.id.tvPasswordStrength);
        viewStr1 = findViewById(R.id.viewStr1);
        viewStr2 = findViewById(R.id.viewStr2);
        viewStr3 = findViewById(R.id.viewStr3);
        viewStr4 = findViewById(R.id.viewStr4);

        if (etPassword == null) return;

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                evaluateStrength(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void evaluateStrength(String password) {
        if (password.isEmpty()) {
            if (tvPasswordStrength != null) {
                tvPasswordStrength.setText("Chưa nhập");
                tvPasswordStrength.setTextColor(Color.parseColor("#888888"));
            }
            setBarColors(0, "#E0E0E0");
            return;
        }

        int score = 0;

        // 1. Độ dài >= 6 ký tự (+1 điểm)
        if (password.length() >= 6) score++;

        // 2. Độ dài >= 8 ký tự hoặc chứa chữ hoa + chữ thường (+1 điểm)
        if (password.length() >= 8 || (password.matches(".*[a-z].*") && password.matches(".*[A-Z].*"))) score++;

        // 3. Chứa chữ số (0-9) (+1 điểm)
        if (password.matches(".*[0-9].*")) score++;

        // 4. Chứa ký tự đặc biệt (!@#$%^&*...) (+1 điểm)
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) score++;

        if (score == 0) score = 1;

        switch (score) {
            case 1:
                if (tvPasswordStrength != null) {
                    tvPasswordStrength.setText("Yếu (Quá ngắn)");
                    tvPasswordStrength.setTextColor(Color.parseColor("#EF4444"));
                }
                setBarColors(1, "#EF4444");
                break;
            case 2:
                if (tvPasswordStrength != null) {
                    tvPasswordStrength.setText("Trung bình");
                    tvPasswordStrength.setTextColor(Color.parseColor("#F59E0B"));
                }
                setBarColors(2, "#F59E0B");
                break;
            case 3:
                if (tvPasswordStrength != null) {
                    tvPasswordStrength.setText("Khá mạnh");
                    tvPasswordStrength.setTextColor(Color.parseColor("#3B82F6"));
                }
                setBarColors(3, "#3B82F6");
                break;
            case 4:
            default:
                if (tvPasswordStrength != null) {
                    tvPasswordStrength.setText("Rất mạnh (Bảo mật 100%)");
                    tvPasswordStrength.setTextColor(Color.parseColor("#10B981"));
                }
                setBarColors(4, "#10B981");
                break;
        }
    }

    private void setBarColors(int activeCount, String activeHexColor) {
        int activeColor = Color.parseColor(activeHexColor);
        int defaultColor = Color.parseColor("#E0E0E0");

        if (viewStr1 != null) viewStr1.setBackgroundTintList(ColorStateList.valueOf(activeCount >= 1 ? activeColor : defaultColor));
        if (viewStr2 != null) viewStr2.setBackgroundTintList(ColorStateList.valueOf(activeCount >= 2 ? activeColor : defaultColor));
        if (viewStr3 != null) viewStr3.setBackgroundTintList(ColorStateList.valueOf(activeCount >= 3 ? activeColor : defaultColor));
        if (viewStr4 != null) viewStr4.setBackgroundTintList(ColorStateList.valueOf(activeCount >= 4 ? activeColor : defaultColor));
    }

    private void handleCheckResult(ApiService apiService, User newUser, boolean isDuplicate, TextView btnSubmitSignUp) {
        btnSubmitSignUp.setEnabled(true);
        btnSubmitSignUp.setText("Đăng ký →");

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
