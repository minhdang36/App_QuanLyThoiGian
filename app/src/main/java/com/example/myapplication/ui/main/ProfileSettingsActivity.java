package com.example.myapplication.ui.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
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
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.model.User;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.ui.auth.LoginActivity;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileSettingsActivity extends AppCompatActivity {

    private ImageView imgProfileAvatar;
    private ImageView dialogAvatarPreview;
    private TextView tvUserName, tvUserEmail;

    private ActivityResultLauncher<String> avatarPickerLauncher;

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private LinearLayout createStyledInputContainer(String hint, EditText et, int iconRes) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER_VERTICAL);
        container.setBackgroundResource(R.drawable.bg_search_bar);
        container.setPadding(dpToPx(16), 0, dpToPx(16), 0);

        LinearLayout.LayoutParams lpContainer = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(50)
        );
        lpContainer.setMargins(0, dpToPx(4), 0, dpToPx(12));
        container.setLayoutParams(lpContainer);

        if (iconRes > 0) {
            ImageView img = new ImageView(this);
            img.setImageResource(iconRes);
            img.setColorFilter(ContextCompat.getColor(this, R.color.brand_primary));
            LinearLayout.LayoutParams lpImg = new LinearLayout.LayoutParams(dpToPx(20), dpToPx(20));
            container.addView(img, lpImg);
        }

        et.setHint(hint);
        et.setBackground(null);
        et.setTextSize(14);
        et.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        et.setHintTextColor(ContextCompat.getColor(this, R.color.text_muted));

        LinearLayout.LayoutParams lpEt = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
        );
        if (iconRes > 0) lpEt.setMargins(dpToPx(12), 0, 0, 0);
        container.addView(et, lpEt);

        return container;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences appPref = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean isDarkMode = appPref.getBoolean("DARK_MODE_ENABLED", false);
        int targetNightMode = isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        if (AppCompatDelegate.getDefaultNightMode() != targetNightMode) {
            AppCompatDelegate.setDefaultNightMode(targetNightMode);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        imgProfileAvatar = findViewById(R.id.imgProfileAvatar);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);

        avatarPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Exception ignored) {}

                        if (imgProfileAvatar != null) imgProfileAvatar.setImageURI(uri);
                        if (dialogAvatarPreview != null) dialogAvatarPreview.setImageURI(uri);

                        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
                        sharedPref.edit().putString("USER_AVATAR_URI", uri.toString()).apply();
                        Toast.makeText(this, "✔ Đã cập nhật ảnh đại diện!", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        loadUserDataFromSession();

        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", 1);
        loadUserDataFromApi(currentUserId);

        setupOptionClicks();
        setupDarkModeSwitch();
    }

    private void setupDarkModeSwitch() {
        Switch switchDarkMode = findViewById(R.id.switchDarkMode);
        if (switchDarkMode != null) {
            SharedPreferences appPref = getSharedPreferences("AppSettings", MODE_PRIVATE);
            boolean isDarkMode = appPref.getBoolean("DARK_MODE_ENABLED", false);

            switchDarkMode.setOnCheckedChangeListener(null);
            switchDarkMode.setChecked(isDarkMode);

            switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                appPref.edit().putBoolean("DARK_MODE_ENABLED", isChecked).apply();
                int targetMode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
                AppCompatDelegate.setDefaultNightMode(targetMode);
                Toast.makeText(this, isChecked ? "🌙 Đã bật Chế độ tối (Dark Mode)" : "☀️ Đã bật Chế độ sáng (Light Mode)", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loadUserDataFromSession() {
        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        String name = sharedPref.getString("USER_NAME", "Minh Đặng");
        String email = sharedPref.getString("USER_EMAIL", "minhdang@flowagenda.vn");
        String avatarUri = sharedPref.getString("USER_AVATAR_URI", "");

        if (tvUserName != null) tvUserName.setText(name);
        if (tvUserEmail != null) tvUserEmail.setText(email);

        if (!avatarUri.isEmpty() && imgProfileAvatar != null) {
            try {
                imgProfileAvatar.setImageURI(Uri.parse(avatarUri));
            } catch (Exception e) {
                imgProfileAvatar.setImageResource(android.R.drawable.ic_menu_myplaces);
            }
        }
    }

    private void setupOptionClicks() {
        View btnEditProfile = findViewById(R.id.btnEditProfile);
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        }

        View btnChangePassword = findViewById(R.id.btnChangePassword);
        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        }

        View btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutConfirmDialog());
        }
    }

    private void showEditProfileDialog() {
        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", 1);
        String currentName = sharedPref.getString("USER_NAME", "Minh Đặng");
        String currentPhone = sharedPref.getString("USER_PHONE", "");
        String currentEmail = sharedPref.getString("USER_EMAIL", "");
        String currentBio = sharedPref.getString("USER_BIO", "");
        String currentAvatar = sharedPref.getString("USER_AVATAR_URI", "");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout dialogCard = new LinearLayout(this);
        dialogCard.setOrientation(LinearLayout.VERTICAL);
        dialogCard.setBackgroundResource(R.drawable.bg_rounded_24dp);
        dialogCard.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.card_bg)));
        dialogCard.setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(24));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("✏️ Chỉnh sửa hồ sơ");
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        dialogCard.addView(tvTitle);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setPadding(0, dpToPx(12), 0, dpToPx(12));
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(layout);

        LinearLayout avatarLayout = new LinearLayout(this);
        avatarLayout.setOrientation(LinearLayout.VERTICAL);
        avatarLayout.setGravity(Gravity.CENTER);
        avatarLayout.setPadding(0, 0, 0, dpToPx(12));

        dialogAvatarPreview = new ImageView(this);
        LinearLayout.LayoutParams lpAvt = new LinearLayout.LayoutParams(dpToPx(72), dpToPx(72));
        dialogAvatarPreview.setLayoutParams(lpAvt);

        if (!currentAvatar.isEmpty()) {
            try {
                dialogAvatarPreview.setImageURI(Uri.parse(currentAvatar));
            } catch (Exception e) {
                dialogAvatarPreview.setImageResource(android.R.drawable.ic_menu_myplaces);
            }
        } else {
            dialogAvatarPreview.setImageResource(android.R.drawable.ic_menu_myplaces);
        }

        TextView tvChangeAvatar = new TextView(this);
        tvChangeAvatar.setText("📷 Đổi ảnh đại diện");
        tvChangeAvatar.setTextColor(ContextCompat.getColor(this, R.color.brand_primary));
        tvChangeAvatar.setTextSize(13);
        tvChangeAvatar.setTypeface(null, Typeface.BOLD);
        tvChangeAvatar.setPadding(0, dpToPx(6), 0, dpToPx(6));
        tvChangeAvatar.setGravity(Gravity.CENTER);

        View.OnClickListener avatarPickListener = v -> avatarPickerLauncher.launch("image/*");
        dialogAvatarPreview.setOnClickListener(avatarPickListener);
        tvChangeAvatar.setOnClickListener(avatarPickListener);

        avatarLayout.addView(dialogAvatarPreview);
        avatarLayout.addView(tvChangeAvatar);
        layout.addView(avatarLayout);

        final EditText etName = new EditText(this);
        etName.setText(currentName);
        layout.addView(createStyledInputContainer("Họ và tên", etName, android.R.drawable.ic_menu_myplaces));

        final EditText etPhone = new EditText(this);
        etPhone.setInputType(InputType.TYPE_CLASS_PHONE);
        etPhone.setText(currentPhone);
        layout.addView(createStyledInputContainer("Số điện thoại", etPhone, android.R.drawable.ic_menu_call));

        final EditText etEmail = new EditText(this);
        etEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etEmail.setText(currentEmail);
        layout.addView(createStyledInputContainer("Địa chỉ Email", etEmail, android.R.drawable.ic_dialog_email));

        final EditText etBio = new EditText(this);
        etBio.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etBio.setText(currentBio);
        layout.addView(createStyledInputContainer("Tiểu sử / Giới thiệu bản thân", etBio, android.R.drawable.ic_menu_edit));

        dialogCard.addView(scrollView);

        builder.setView(dialogCard);
        AlertDialog dialog = builder.create();

        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);

        TextView btnCancel = new TextView(this);
        btnCancel.setText("HỦY");
        btnCancel.setTextSize(13);
        btnCancel.setTypeface(null, Typeface.BOLD);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        btnCancel.setBackgroundResource(R.drawable.bg_pill_light);
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        TextView btnSave = new TextView(this);
        btnSave.setText("LƯU THAY ĐỔI");
        btnSave.setTextSize(13);
        btnSave.setTypeface(null, Typeface.BOLD);
        btnSave.setGravity(Gravity.CENTER);
        btnSave.setTextColor(Color.WHITE);
        btnSave.setBackgroundResource(R.drawable.bg_pill_brown);
        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            String newPhone = etPhone.getText().toString().trim();
            String newEmail = etEmail.getText().toString().trim();
            String newBio = etBio.getText().toString().trim();
            String latestAvatar = sharedPref.getString("USER_AVATAR_URI", "");

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

            User updateData = new User();
            updateData.setFullName(newName);
            if (!newEmail.isEmpty()) updateData.setEmail(newEmail);
            updateData.setPhone(newPhone);
            updateData.setBio(newBio);
            updateData.setAvatarUrl(latestAvatar);

            RetrofitClient.getApiService().updateUserProfile(currentUserId, updateData).enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    Toast.makeText(ProfileSettingsActivity.this, "✔ Đã đồng bộ hồ sơ lên máy chủ!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    Toast.makeText(ProfileSettingsActivity.this, "✔ Đã lưu hồ sơ trên thiết bị!", Toast.LENGTH_SHORT).show();
                }
            });

            dialog.dismiss();
        });

        LinearLayout.LayoutParams lpCancel = new LinearLayout.LayoutParams(0, dpToPx(48), 1f);
        lpCancel.setMargins(0, 0, dpToPx(8), 0);
        btnCancel.setLayoutParams(lpCancel);

        LinearLayout.LayoutParams lpSave = new LinearLayout.LayoutParams(0, dpToPx(48), 1.5f);
        lpSave.setMargins(dpToPx(8), 0, 0, 0);
        btnSave.setLayoutParams(lpSave);

        btnLayout.addView(btnCancel);
        btnLayout.addView(btnSave);
        dialogCard.addView(btnLayout);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    private void showChangePasswordDialog() {
        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", 1);
        boolean isBiometricOn = sharedPref.getBoolean("BIOMETRIC_ENABLED", false);
        boolean is2FAOn = sharedPref.getBoolean("TWO_FACTOR_ENABLED", false);

        int textPrimaryColor = ContextCompat.getColor(this, R.color.text_primary);
        int textSecondaryColor = ContextCompat.getColor(this, R.color.text_secondary);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout dialogCard = new LinearLayout(this);
        dialogCard.setOrientation(LinearLayout.VERTICAL);
        dialogCard.setBackgroundResource(R.drawable.bg_rounded_24dp);
        dialogCard.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.card_bg)));
        dialogCard.setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(24));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("🛡 Mật khẩu & Bảo mật 2 lớp");
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(textPrimaryColor);
        dialogCard.addView(tvTitle);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setPadding(0, dpToPx(12), 0, dpToPx(12));
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(layout);

        TextView tvHeaderPass = new TextView(this);
        tvHeaderPass.setText("🔑 ĐỔI MẬT KHẨU TÀI KHOẢN");
        tvHeaderPass.setTextSize(11);
        tvHeaderPass.setTypeface(null, Typeface.BOLD);
        tvHeaderPass.setTextColor(ContextCompat.getColor(this, R.color.brand_primary));
        tvHeaderPass.setPadding(0, dpToPx(4), 0, dpToPx(6));
        layout.addView(tvHeaderPass);

        final EditText etOldPass = new EditText(this);
        etOldPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(createStyledInputContainer("Mật khẩu hiện tại", etOldPass, android.R.drawable.ic_lock_idle_lock));

        final EditText etNewPass = new EditText(this);
        etNewPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(createStyledInputContainer("Mật khẩu mới", etNewPass, android.R.drawable.ic_lock_idle_lock));

        final EditText etConfirmPass = new EditText(this);
        etConfirmPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(createStyledInputContainer("Xác nhận mật khẩu mới", etConfirmPass, android.R.drawable.ic_lock_idle_lock));

        TextView tvHeaderSec = new TextView(this);
        tvHeaderSec.setText("🛡 SINH TRẮC HỌC & BẢO MẬT 2 LỚP");
        tvHeaderSec.setTextSize(11);
        tvHeaderSec.setTypeface(null, Typeface.BOLD);
        tvHeaderSec.setTextColor(ContextCompat.getColor(this, R.color.brand_primary));
        tvHeaderSec.setPadding(0, dpToPx(12), 0, dpToPx(6));
        layout.addView(tvHeaderSec);

        LinearLayout bioLayout = new LinearLayout(this);
        bioLayout.setOrientation(LinearLayout.HORIZONTAL);
        bioLayout.setGravity(Gravity.CENTER_VERTICAL);
        bioLayout.setPadding(0, dpToPx(8), 0, dpToPx(8));

        LinearLayout bioTextLayout = new LinearLayout(this);
        bioTextLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams weightParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        bioTextLayout.setLayoutParams(weightParams);

        TextView tvBioTitle = new TextView(this);
        tvBioTitle.setText("Xác thực Sinh trắc học (Vân tay)");
        tvBioTitle.setTextColor(textPrimaryColor);
        tvBioTitle.setTextSize(13);
        tvBioTitle.setTypeface(null, Typeface.BOLD);

        TextView tvBioSub = new TextView(this);
        tvBioSub.setText("Đăng nhập ứng dụng bằng Vân tay / Face ID");
        tvBioSub.setTextColor(textSecondaryColor);
        tvBioSub.setTextSize(11);

        bioTextLayout.addView(tvBioTitle);
        bioTextLayout.addView(tvBioSub);

        Switch switchBiometric = new Switch(this);
        switchBiometric.setChecked(isBiometricOn);

        bioLayout.addView(bioTextLayout);
        bioLayout.addView(switchBiometric);
        layout.addView(bioLayout);

        LinearLayout twoFaLayout = new LinearLayout(this);
        twoFaLayout.setOrientation(LinearLayout.HORIZONTAL);
        twoFaLayout.setGravity(Gravity.CENTER_VERTICAL);
        twoFaLayout.setPadding(0, dpToPx(8), 0, dpToPx(8));

        LinearLayout twoFaTextLayout = new LinearLayout(this);
        twoFaTextLayout.setOrientation(LinearLayout.VERTICAL);
        twoFaTextLayout.setLayoutParams(weightParams);

        TextView tv2FATitle = new TextView(this);
        tv2FATitle.setText("Bảo mật 2 lớp (Xác minh 2 bước)");
        tv2FATitle.setTextColor(textPrimaryColor);
        tv2FATitle.setTextSize(13);
        tv2FATitle.setTypeface(null, Typeface.BOLD);

        TextView tv2FASub = new TextView(this);
        tv2FASub.setText("Gửi mã OTP xác nhận về Email khi đăng nhập");
        tv2FASub.setTextColor(textSecondaryColor);
        tv2FASub.setTextSize(11);

        twoFaTextLayout.addView(tv2FATitle);
        twoFaTextLayout.addView(tv2FASub);

        Switch switchTwoFactor = new Switch(this);
        switchTwoFactor.setChecked(is2FAOn);

        twoFaLayout.addView(twoFaTextLayout);
        twoFaLayout.addView(switchTwoFactor);
        layout.addView(twoFaLayout);

        dialogCard.addView(scrollView);

        builder.setView(dialogCard);
        AlertDialog dialog = builder.create();

        LinearLayout btnLayout = new LinearLayout(this);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);

        TextView btnCancel = new TextView(this);
        btnCancel.setText("HỦY");
        btnCancel.setTextSize(13);
        btnCancel.setTypeface(null, Typeface.BOLD);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setTextColor(textSecondaryColor);
        btnCancel.setBackgroundResource(R.drawable.bg_pill_light);
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        TextView btnSave = new TextView(this);
        btnSave.setText("LƯU CÀI ĐẶT");
        btnSave.setTextSize(13);
        btnSave.setTypeface(null, Typeface.BOLD);
        btnSave.setGravity(Gravity.CENTER);
        btnSave.setTextColor(Color.WHITE);
        btnSave.setBackgroundResource(R.drawable.bg_pill_brown);
        btnSave.setOnClickListener(v -> {
            String oldPass = etOldPass.getText().toString().trim();
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

            if (!newPass.isEmpty()) {
                Map<String, String> payload = new HashMap<>();
                payload.put("oldPassword", oldPass);
                payload.put("newPassword", newPass);

                RetrofitClient.getApiService().changePassword(currentUserId, payload).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(ProfileSettingsActivity.this, "✔ Đã cập nhật mật khẩu và bảo mật thành công!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(ProfileSettingsActivity.this, "⚠️ Mật khẩu hiện tại không đúng!", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Toast.makeText(ProfileSettingsActivity.this, "❌ Không thể kết nối máy chủ để đổi mật khẩu!", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                String msg = "✔ Đã cập nhật bảo mật!";
                if (bioChecked) msg += " • Bật Sinh trắc học";
                if (twoFaChecked) msg += " • Bật 2FA";
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }

            dialog.dismiss();
        });

        LinearLayout.LayoutParams lpCancel = new LinearLayout.LayoutParams(0, dpToPx(48), 1f);
        lpCancel.setMargins(0, 0, dpToPx(8), 0);
        btnCancel.setLayoutParams(lpCancel);

        LinearLayout.LayoutParams lpSave = new LinearLayout.LayoutParams(0, dpToPx(48), 1.5f);
        lpSave.setMargins(dpToPx(8), 0, 0, 0);
        btnSave.setLayoutParams(lpSave);

        btnLayout.addView(btnCancel);
        btnLayout.addView(btnSave);
        dialogCard.addView(btnLayout);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    private void loadUserDataFromApi(int userId) {
        ApiService apiService = RetrofitClient.getApiService();
        apiService.getUserProfile(userId).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    if (tvUserName != null && user.getFullName() != null) tvUserName.setText(user.getFullName());
                    if (tvUserEmail != null && user.getEmail() != null) tvUserEmail.setText(user.getEmail());

                    SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    if (user.getFullName() != null) editor.putString("USER_NAME", user.getFullName());
                    if (user.getEmail() != null) editor.putString("USER_EMAIL", user.getEmail());
                    if (user.getPhone() != null) editor.putString("USER_PHONE", user.getPhone());
                    if (user.getBio() != null) editor.putString("USER_BIO", user.getBio());
                    editor.apply();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {}
        });
    }

    private void showLogoutConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản không?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
                    sharedPref.edit().clear().apply();

                    Toast.makeText(this, "Đã đăng xuất thành công!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(ProfileSettingsActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
