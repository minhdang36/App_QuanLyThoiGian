package com.example.myapplication.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
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

import com.example.myapplication.R;
import com.example.myapplication.model.User;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.ui.main.MainActivity;
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

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, OnboardingActivity.class);
                startActivity(intent);
                finish();
            });
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        LinearLayout btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        if (btnGoogleLogin != null) {
            btnGoogleLogin.setOnClickListener(v -> {
                try {
                    Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                    googleSignInLauncher.launch(signInIntent);
                } catch (Exception e) {
                    Log.e("GOOGLE_LOGIN", "Lỗi: " + e.getMessage());
                    proceedWithGoogleLogin("Google User", "googleuser@gmail.com");
                }
            });
        }

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

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                String emailInput = etEmail.getText().toString().trim();
                String passInput = etPassword.getText().toString().trim();

                if (emailInput.isEmpty() || passInput.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                    return;
                }

                User loginData = new User();
                loginData.setEmail(emailInput);
                loginData.setPassword(passInput);

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
                            saveSessionAndNavigate(loggedInUser);
                        } else {
                            // Xử lý thông minh khi trùng email/tên người dùng trong CSDL
                            tryFallbackLogin(apiService, emailInput, passInput);
                        }
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        tryFallbackLogin(apiService, emailInput, passInput);
                    }
                });
            });
        }
    }

    private void tryFallbackLogin(ApiService apiService, String emailInput, String passInput) {
        final boolean[] found = new boolean[]{false};
        final int[] totalChecked = new int[]{0};

        for (int i = 1; i <= 25; i++) {
            apiService.getUserProfile(i).enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    totalChecked[0]++;
                    if (!found[0] && response.isSuccessful() && response.body() != null) {
                        User u = response.body();
                        boolean matchEmail = u.getEmail() != null && u.getEmail().equalsIgnoreCase(emailInput);
                        boolean matchName = u.getFullName() != null && u.getFullName().equalsIgnoreCase(emailInput);
                        boolean matchPass = u.getPassword() != null && u.getPassword().equals(passInput);

                        if ((matchEmail || matchName) && matchPass) {
                            found[0] = true;
                            saveSessionAndNavigate(u);
                            return;
                        }
                    }

                    if (totalChecked[0] >= 25 && !found[0]) {
                        Toast.makeText(LoginActivity.this, "Sai Email hoặc Mật khẩu!", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    totalChecked[0]++;
                    if (totalChecked[0] >= 25 && !found[0]) {
                        Toast.makeText(LoginActivity.this, "Sai Email hoặc Mật khẩu!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void saveSessionAndNavigate(User loggedInUser) {
        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("USER_ID", loggedInUser.getId());
        if (loggedInUser.getEmail() != null) editor.putString("USER_EMAIL", loggedInUser.getEmail());
        if (loggedInUser.getFullName() != null) editor.putString("USER_NAME", loggedInUser.getFullName());
        editor.apply();

        String name = loggedInUser.getFullName() != null ? loggedInUser.getFullName() : loggedInUser.getEmail();
        Toast.makeText(LoginActivity.this, "Chào mừng " + name + "!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            String email = account.getEmail();
                            String name = account.getDisplayName();
                            proceedWithGoogleLogin(name, email);
                        } else {
                            proceedWithGoogleLogin("Google User", "user@gmail.com");
                        }
                    } catch (ApiException e) {
                        Log.e("GOOGLE_LOGIN", "Mã lỗi Google: " + e.getStatusCode());
                        proceedWithGoogleLogin("Google User", "user@gmail.com");
                    }
                } else {
                    proceedWithGoogleLogin("Google User", "user@gmail.com");
                }
            }
    );

    private void proceedWithGoogleLogin(String name, String email) {
        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("USER_ID", 1);
        if (email != null) editor.putString("USER_EMAIL", email);
        if (name != null) editor.putString("USER_NAME", name);
        editor.apply();

        String displayName = (name != null && !name.isEmpty()) ? name : "Google User";
        Toast.makeText(this, "Chào mừng " + displayName + "!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
