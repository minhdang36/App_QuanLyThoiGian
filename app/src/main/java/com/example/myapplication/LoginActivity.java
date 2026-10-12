package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

public class LoginActivity extends AppCompatActivity {

    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // ==========================================
        // 1. XỬ LÝ NÚT QUAY LẠI (BACK)
        // ==========================================
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                // Mở lại trang Onboarding
                Intent intent = new Intent(LoginActivity.this, OnboardingActivity.class);
                startActivity(intent);
                finish(); // Đóng trang Login hiện tại
            });
        }

        // ==========================================
        // 2. XỬ LÝ ĐĂNG NHẬP BẰNG GOOGLE
        // ==========================================
        // Cấu hình yêu cầu lấy Email và Thông tin cơ bản từ tài khoản Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        LinearLayout btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        if (btnGoogleLogin != null) {
            btnGoogleLogin.setOnClickListener(v -> {
                // Mở cửa sổ chọn tài khoản Google của hệ điều hành
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            });
        }

        // ==========================================
        // CÁC CHỨC NĂNG CŨ (ĐĂNG KÝ VÀ ĐĂNG NHẬP THƯỜNG)
        // ==========================================
        TextView tvSignUp = findViewById(R.id.tvSignUp);
        if (tvSignUp != null) {
            tvSignUp.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            });
        }

        EditText etEmail = findViewById(R.id.etLoginEmail);
        EditText etPassword = findViewById(R.id.etLoginPassword);
        TextView btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Tạo đối tượng chứa dữ liệu gửi lên
            User loginData = new User();
            loginData.setEmail(email);
            loginData.setPassword(pass);

            // Gọi API Đăng nhập thật
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://10.0.2.2:8080/api/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ApiService apiService = retrofit.create(ApiService.class);
            apiService.loginUser(loginData).enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        User loggedInUser = response.body();

                        // LƯU USER_ID VÀO BỘ NHỚ MÁY (SharedPreferences)
                        android.content.SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
                        android.content.SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putInt("USER_ID", loggedInUser.getId()); // Lưu ID của người này lại
                        editor.apply(); // Xác nhận lưu

                        Toast.makeText(LoginActivity.this, "Chào mừng trở lại!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Sai Email hoặc Mật khẩu!", Toast.LENGTH_SHORT).show();
                    }
                }

                // ĐÃ BỔ SUNG HÀM onFailure BỊ THIẾU
                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    Toast.makeText(LoginActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
                }
            }); // Đóng ngoặc API Call
        }); // Đóng ngoặc sự kiện btnLogin
    } // Đóng ngoặc hàm onCreate

    // ==========================================
    // 3. HÀM NHẬN KẾT QUẢ TỪ GOOGLE TRẢ VỀ
    // ==========================================
    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        // Lấy thông tin tài khoản thành công
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            String email = account.getEmail();
                            String name = account.getDisplayName();

                            // Hiện thông báo chào mừng tên người dùng
                            Toast.makeText(this, "Chào " + name, Toast.LENGTH_LONG).show();

                            // Chuyển thẳng vào Màn hình chính
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    } catch (ApiException e) {
                        Log.e("GOOGLE_LOGIN", "Đăng nhập Google thất bại, mã lỗi: " + e.getStatusCode());
                        Toast.makeText(this, "Đăng nhập Google thất bại!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );
}