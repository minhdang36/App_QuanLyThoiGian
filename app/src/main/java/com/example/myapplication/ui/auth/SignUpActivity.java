package com.example.myapplication.ui.auth;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
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
import com.example.myapplication.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
        ImageView imgTogglePassword = findViewById(R.id.imgTogglePassword);
        TextView btnSubmitSignUp = findViewById(R.id.btnSubmitSignUp);

        if (imgTogglePassword != null && etSignUpPassword != null) {
            imgTogglePassword.setOnClickListener(v -> {
                int inputType = etSignUpPassword.getInputType();
                if ((inputType & InputType.TYPE_TEXT_VARIATION_PASSWORD) == InputType.TYPE_TEXT_VARIATION_PASSWORD) {
                    etSignUpPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    imgTogglePassword.setAlpha(1.0f);
                } else {
                    etSignUpPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    imgTogglePassword.setAlpha(0.5f);
                }
                etSignUpPassword.setSelection(etSignUpPassword.getText().length());
            });
        }

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

                ApiService apiService = RetrofitClient.getApiService();

                btnSubmitSignUp.setEnabled(false);
                btnSubmitSignUp.setText("Đang đăng ký...");

                executeRegister(apiService, newUser, btnSubmitSignUp);
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

        if (password.length() >= 6) score++;
        if (password.length() >= 8 || (password.matches(".*[a-z].*") && password.matches(".*[A-Z].*"))) score++;
        if (password.matches(".*[0-9].*")) score++;
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

    private void executeRegister(ApiService apiService, User newUser, TextView btnSubmitSignUp) {
        apiService.registerUser(newUser).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                btnSubmitSignUp.setEnabled(true);
                btnSubmitSignUp.setText("Đăng ký →");

                if (response.isSuccessful()) {
                    Toast.makeText(SignUpActivity.this, "✔ Đăng ký thành công! Hãy đăng nhập.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else if (response.code() == 409) {
                    Toast.makeText(SignUpActivity.this, "⚠️ Email hoặc Tên người dùng này đã tồn tại! Vui lòng chọn tên khác.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(SignUpActivity.this, "⚠️ Không thể đăng ký tài khoản (Mã lỗi: " + response.code() + ")", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                btnSubmitSignUp.setEnabled(true);
                btnSubmitSignUp.setText("Đăng ký →");
                Toast.makeText(SignUpActivity.this, "❌ Không thể kết nối tới máy chủ! Vui lòng kiểm tra lại Server.", Toast.LENGTH_LONG).show();
            }
        });
    }
}
