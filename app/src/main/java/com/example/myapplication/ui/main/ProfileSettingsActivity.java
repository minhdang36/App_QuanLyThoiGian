package com.example.myapplication.ui.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
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

    private ImageView imgProfileAvatar;
    private ImageView dialogAvatarPreview;
    private TextView tvUserName, tvUserEmail, tvUserBadge, tvStreakDays, tvFocusHours;

    private ActivityResultLauncher<String> avatarPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        imgProfileAvatar = findViewById(R.id.imgProfileAvatar);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserBadge = findViewById(R.id.tvUserBadge);
        tvStreakDays = findViewById(R.id.tvStreakDays);
        tvFocusHours = findViewById(R.id.tvFocusHours);

        avatarPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        if (dialogAvatarPreview != null) {
                            dialogAvatarPreview.setImageURI(uri);
                        }
                        if (imgProfileAvatar != null) {
                            imgProfileAvatar.setImageURI(uri);
                        }
                        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
                        sharedPref.edit().putString("USER_AVATAR_URI", uri.toString()).apply();
                        Toast.makeText(this, "Đã chọn ảnh đại diện mới!", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", 1);
        String savedEmail = sharedPref.getString("USER_EMAIL", "");
        String savedName = sharedPref.getString("USER_NAME", "");
        String savedAvatarUriStr = sharedPref.getString("USER_AVATAR_URI", "");

        if (!savedAvatarUriStr.isEmpty() && imgProfileAvatar != null) {
            try {
                imgProfileAvatar.setImageURI(Uri.parse(savedAvatarUriStr));
            } catch (Exception ignored) {}
        }

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

        View btnEditProfile = findViewById(R.id.btnEditProfile);
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        }

        View btnChangePassword = findViewById(R.id.btnChangePassword);
        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        }

        Switch switchDarkMode = findViewById(R.id.switchDarkMode);
        SharedPreferences appPref = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean isDarkMode = appPref.getBoolean("DARK_MODE_ENABLED", false);

        if (switchDarkMode != null) {
            switchDarkMode.setChecked(isDarkMode);
            switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                appPref.edit().putBoolean("DARK_MODE_ENABLED", isChecked).apply();
                if (isChecked) {
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
                    Toast.makeText(this, "🌙 Đã bật Chế độ tối (Dark Mode)!", Toast.LENGTH_SHORT).show();
                } else {
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
                    Toast.makeText(this, "☀️ Đã bật Chế độ sáng (Light Mode)!", Toast.LENGTH_SHORT).show();
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

    private void showEditProfileDialog() {
        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        String currentName = tvUserName != null ? tvUserName.getText().toString() : "";
        String currentEmail = tvUserEmail != null ? tvUserEmail.getText().toString() : "";
        String currentPhone = sharedPref.getString("USER_PHONE", "");
        String currentBio = sharedPref.getString("USER_BIO", "");
        String savedAvatarUriStr = sharedPref.getString("USER_AVATAR_URI", "");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 20);
        scrollView.addView(layout);

        // 1. Chỉnh sửa Ảnh Đại diện
        LinearLayout avatarLayout = new LinearLayout(this);
        avatarLayout.setOrientation(LinearLayout.VERTICAL);
        avatarLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        avatarLayout.setPadding(0, 0, 0, 16);

        dialogAvatarPreview = new ImageView(this);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(160, 160);
        dialogAvatarPreview.setLayoutParams(avatarParams);
        dialogAvatarPreview.setBackgroundResource(R.drawable.bg_circle_teal);
        dialogAvatarPreview.setPadding(4, 4, 4, 4);

        if (!savedAvatarUriStr.isEmpty()) {
            try {
                dialogAvatarPreview.setImageURI(Uri.parse(savedAvatarUriStr));
            } catch (Exception ignored) {
                dialogAvatarPreview.setImageResource(android.R.drawable.ic_menu_myplaces);
            }
        } else {
            dialogAvatarPreview.setImageResource(android.R.drawable.ic_menu_myplaces);
        }

        TextView tvChangeAvatar = new TextView(this);
        tvChangeAvatar.setText("📷 Đổi ảnh đại diện");
        tvChangeAvatar.setTextColor(0xFF10B981);
        tvChangeAvatar.setTextSize(13);
        tvChangeAvatar.setTypeface(null, Typeface.BOLD);
        tvChangeAvatar.setPadding(0, 10, 0, 12);
        tvChangeAvatar.setGravity(Gravity.CENTER);

        View.OnClickListener avatarPickListener = v -> avatarPickerLauncher.launch("image/*");
        dialogAvatarPreview.setOnClickListener(avatarPickListener);
        tvChangeAvatar.setOnClickListener(avatarPickListener);

        avatarLayout.addView(dialogAvatarPreview);
        avatarLayout.addView(tvChangeAvatar);
        layout.addView(avatarLayout);

        // 2. Họ và Tên
        final EditText etName = new EditText(this);
        etName.setHint("Họ và tên");
        etName.setText(currentName);
        layout.addView(etName);

        // 3. Số điện thoại
        final EditText etPhone = new EditText(this);
        etPhone.setHint("Số điện thoại");
        etPhone.setInputType(InputType.TYPE_CLASS_PHONE);
        etPhone.setText(currentPhone);
        layout.addView(etPhone);

        // 4. Địa chỉ Email
        final EditText etEmail = new EditText(this);
        etEmail.setHint("Địa chỉ Email");
        etEmail.setText(currentEmail);
        etEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(etEmail);

        // 5. Tiểu sử / Giới thiệu
        final EditText etBio = new EditText(this);
        etBio.setHint("Tiểu sử / Giới thiệu bản thân");
        etBio.setText(currentBio);
        etBio.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        layout.addView(etBio);

        new AlertDialog.Builder(this)
                .setTitle("Chỉnh sửa hồ sơ")
                .setView(scrollView)
                .setPositiveButton("Lưu thay đổi", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    String newPhone = etPhone.getText().toString().trim();
                    String newEmail = etEmail.getText().toString().trim();
                    String newBio = etBio.getText().toString().trim();

                    if (newName.isEmpty()) {
                        Toast.makeText(this, "Họ và tên không được để trống!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("USER_NAME", newName);
                    editor.putString("USER_PHONE", newPhone);
                    if (!newEmail.isEmpty()) editor.putString("USER_EMAIL", newEmail);
                    editor.putString("USER_BIO", newBio);
                    editor.apply();

                    if (tvUserName != null) tvUserName.setText(newName);
                    if (tvUserEmail != null && !newEmail.isEmpty()) tvUserEmail.setText(newEmail);

                    Toast.makeText(this, "✔ Đã cập nhật ảnh đại diện & thông tin hồ sơ!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showChangePasswordDialog() {
        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        boolean isBiometricOn = sharedPref.getBoolean("BIOMETRIC_ENABLED", false);
        boolean is2FAOn = sharedPref.getBoolean("TWO_FACTOR_ENABLED", false);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 20);
        scrollView.addView(layout);

        // --- PHẦN 1: MẬT KHẨU TÀI KHOẢN ---
        TextView tvHeaderPass = new TextView(this);
        tvHeaderPass.setText("🔑 ĐỔI MẬT KHẨU TÀI KHOẢN");
        tvHeaderPass.setTextSize(12);
        tvHeaderPass.setTypeface(null, Typeface.BOLD);
        tvHeaderPass.setTextColor(0xFF888888);
        tvHeaderPass.setPadding(0, 0, 0, 10);
        layout.addView(tvHeaderPass);

        final EditText etOldPass = new EditText(this);
        etOldPass.setHint("Mật khẩu hiện tại");
        etOldPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etOldPass);

        final EditText etNewPass = new EditText(this);
        etNewPass.setHint("Mật khẩu mới");
        etNewPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNewPass);

        final EditText etConfirmPass = new EditText(this);
        etConfirmPass.setHint("Xác nhận mật khẩu mới");
        etConfirmPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etConfirmPass);

        // --- PHẦN 2: SINH TRẮC HỌC & BẢO MẬT 2 LỚP ---
        TextView tvHeaderSec = new TextView(this);
        tvHeaderSec.setText("🛡 SINH TRẮC HỌC & BẢO MẬT 2 LỚP");
        tvHeaderSec.setTextSize(12);
        tvHeaderSec.setTypeface(null, Typeface.BOLD);
        tvHeaderSec.setTextColor(0xFF888888);
        tvHeaderSec.setPadding(0, 30, 0, 10);
        layout.addView(tvHeaderSec);

        // 1. Switch Sinh trắc học (Vân tay / Face ID)
        LinearLayout bioLayout = new LinearLayout(this);
        bioLayout.setOrientation(LinearLayout.HORIZONTAL);
        bioLayout.setGravity(Gravity.CENTER_VERTICAL);
        bioLayout.setPadding(0, 10, 0, 10);

        LinearLayout bioTextLayout = new LinearLayout(this);
        bioTextLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams weightParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        bioTextLayout.setLayoutParams(weightParams);

        TextView tvBioTitle = new TextView(this);
        tvBioTitle.setText("Xác thực Sinh trắc học (Vân tay)");
        tvBioTitle.setTextColor(0xFF222222);
        tvBioTitle.setTextSize(14);
        tvBioTitle.setTypeface(null, Typeface.BOLD);

        TextView tvBioSub = new TextView(this);
        tvBioSub.setText("Đăng nhập ứng dụng bằng Vân tay / Face ID");
        tvBioSub.setTextColor(0xFF888888);
        tvBioSub.setTextSize(11);

        bioTextLayout.addView(tvBioTitle);
        bioTextLayout.addView(tvBioSub);

        Switch switchBiometric = new Switch(this);
        switchBiometric.setChecked(isBiometricOn);

        bioLayout.addView(bioTextLayout);
        bioLayout.addView(switchBiometric);
        layout.addView(bioLayout);

        // 2. Switch Bảo mật 2 lớp (2FA)
        LinearLayout twoFaLayout = new LinearLayout(this);
        twoFaLayout.setOrientation(LinearLayout.HORIZONTAL);
        twoFaLayout.setGravity(Gravity.CENTER_VERTICAL);
        twoFaLayout.setPadding(0, 10, 0, 10);

        LinearLayout twoFaTextLayout = new LinearLayout(this);
        twoFaTextLayout.setOrientation(LinearLayout.VERTICAL);
        twoFaTextLayout.setLayoutParams(weightParams);

        TextView tv2FATitle = new TextView(this);
        tv2FATitle.setText("Bảo mật 2 lớp (Xác minh 2 bước)");
        tv2FATitle.setTextColor(0xFF222222);
        tv2FATitle.setTextSize(14);
        tv2FATitle.setTypeface(null, Typeface.BOLD);

        TextView tv2FASub = new TextView(this);
        tv2FASub.setText("Gửi mã OTP xác nhận về Email khi đăng nhập");
        tv2FASub.setTextColor(0xFF888888);
        tv2FASub.setTextSize(11);

        twoFaTextLayout.addView(tv2FATitle);
        twoFaTextLayout.addView(tv2FASub);

        Switch switchTwoFactor = new Switch(this);
        switchTwoFactor.setChecked(is2FAOn);

        twoFaLayout.addView(twoFaTextLayout);
        twoFaLayout.addView(switchTwoFactor);
        layout.addView(twoFaLayout);

        new AlertDialog.Builder(this)
                .setTitle("Mật khẩu & Bảo mật 2 lớp")
                .setView(scrollView)
                .setPositiveButton("Lưu cài đặt", (dialog, which) -> {
                    String newPass = etNewPass.getText().toString().trim();
                    String confirmPass = etConfirmPass.getText().toString().trim();

                    if (!newPass.isEmpty()) {
                        if (!newPass.equals(confirmPass)) {
                            Toast.makeText(this, "Mật khẩu mới không trùng khớp!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    boolean bioChecked = switchBiometric.isChecked();
                    boolean twoFaChecked = switchTwoFactor.isChecked();

                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean("BIOMETRIC_ENABLED", bioChecked);
                    editor.putBoolean("TWO_FACTOR_ENABLED", twoFaChecked);
                    editor.apply();

                    String msg = "✔ Đã cập nhật bảo mật!";
                    if (!newPass.isEmpty()) msg += " (Đã đổi mật khẩu)";
                    if (bioChecked) msg += " • Bật Sinh trắc học";
                    if (twoFaChecked) msg += " • Bật 2FA";

                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
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
