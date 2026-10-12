package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.model.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SignUpActivity extends AppCompatActivity {

    private View viewStr1, viewStr2, viewStr3, viewStr4;
    private TextView tvPasswordStrength;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Ánh xạ giao diện
        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvLoginHere = findViewById(R.id.tvLoginHere);
        TextView btnSubmitSignUp = findViewById(R.id.btnSubmitSignUp);
        EditText etFullName = findViewById(R.id.etFullName);
        EditText etEmail = findViewById(R.id.etSignUpEmail);
        EditText etPassword = findViewById(R.id.etSignUpPassword);
        LinearLayout btnGoogleSignUp = findViewById(R.id.btnGoogleSignUp);

        viewStr1 = findViewById(R.id.viewStr1);
        viewStr2 = findViewById(R.id.viewStr2);
        viewStr3 = findViewById(R.id.viewStr3);
        viewStr4 = findViewById(R.id.viewStr4);
        tvPasswordStrength = findViewById(R.id.tvPasswordStrength);

        // 1. CHUYỂN HƯỚNG CƠ BẢN
        btnBack.setOnClickListener(v -> finish());
        tvLoginHere.setOnClickListener(v -> finish());

        // ==========================================
        // 2. LOGIC ĐO ĐỘ MẠNH MẬT KHẨU THEO THỜI GIAN THỰC
        // ==========================================
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculatePasswordStrength(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // ==========================================
        // 3. LOGIC ĐĂNG KÝ BẰNG SQL (GỌI API BACKEND)
        // ==========================================
        btnSubmitSignUp.setOnClickListener(v -> {
            String name = etFullName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Đóng gói dữ liệu gửi lên Spring Boot
            User newUser = new User();
            newUser.setFullName(name);
            newUser.setEmail(email);
            newUser.setPassword(pass);

            registerUserViaAPI(newUser);
        });

        // ==========================================
        // 4. LOGIC ĐĂNG KÝ BẰNG GOOGLE
        // ==========================================
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        if (btnGoogleSignUp != null) {
            btnGoogleSignUp.setOnClickListener(v -> {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignUpLauncher.launch(signInIntent);
            });
        }
    }

    // ==========================================
    // CÁC HÀM XỬ LÝ PHỤ TRỢ BÊN DƯỚI
    // ==========================================

    // Hàm đo độ mạnh mật khẩu và đổi màu thanh Bar
    private void calculatePasswordStrength(String password) {
        String colorGray = "#E0E0E0";
        String colorRed = "#D32F2F";
        String colorYellow = "#FBC02D";
        String colorGreen = "#00695C";

        if (password.isEmpty()) {
            updateStrengthUI(colorGray, colorGray, colorGray, colorGray, "Chưa nhập", "#888888");
            return;
        }

        int score = 0;
        if (password.length() >= 6) score++;
        if (password.matches(".*[a-zA-Z].*")) score++;
        if (password.matches(".*\\d.*")) score++;
        if (password.matches(".*[!@#$%^&*+=?-].*") && password.length() >= 8) score++;

        switch (score) {
            case 1:
            case 2:
                updateStrengthUI(colorRed, colorGray, colorGray, colorGray, "Yếu", colorRed);
                break;
            case 3:
                updateStrengthUI(colorYellow, colorYellow, colorYellow, colorGray, "Trung bình", colorYellow);
                break;
            case 4:
                updateStrengthUI(colorGreen, colorGreen, colorGreen, colorGreen, "Tuyệt vời!", colorGreen);
                break;
            default:
                updateStrengthUI(colorRed, colorGray, colorGray, colorGray, "Rất yếu", colorRed);
                break;
        }
    }

    private void updateStrengthUI(String c1, String c2, String c3, String c4, String text, String textColor) {
        // Dùng setBackgroundTintList để giữ lại hình dáng bo tròn của thanh màu
        viewStr1.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor(c1)));
        viewStr2.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor(c2)));
        viewStr3.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor(c3)));
        viewStr4.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor(c4)));

        tvPasswordStrength.setText(text);
        tvPasswordStrength.setTextColor(Color.parseColor(textColor));
    }

    // Hàm gọi API Đăng ký
    private void registerUserViaAPI(User user) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        apiService.registerUser(user).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SignUpActivity.this, "Tạo tài khoản MySQL thành công!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(SignUpActivity.this, "Đăng ký thất bại!", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(SignUpActivity.this, "Lỗi kết nối Server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Hàm nhận kết quả từ Google
    private final ActivityResultLauncher<Intent> googleSignUpLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            String email = account.getEmail();
                            String name = account.getDisplayName();
                            Toast.makeText(this, "Đăng ký Google thành công: " + name, Toast.LENGTH_LONG).show();

                            // (Tùy chọn: Bạn có thể gọi API lưu email này xuống MySQL ở đây nếu muốn)

                            Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    } catch (ApiException e) {
                        Log.e("GOOGLE_SIGNUP", "Đăng ký Google thất bại, mã lỗi: " + e.getStatusCode());
                        Toast.makeText(this, "Đăng ký Google thất bại!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );
}