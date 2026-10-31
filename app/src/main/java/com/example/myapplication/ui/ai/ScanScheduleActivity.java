package com.example.myapplication.ui.ai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.Task;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.ui.main.MainActivity;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ScanScheduleActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ImageView imgSchedulePreview;
    private Button btnPickGallery, btnAnalyzeSchedule, btnImportSchedule;
    private EditText edtStartDate, edtEndDate;

    private TextView tvSubjectCount, tvDateTitle;
    private TextView tvClass1Period, tvClass1Title, tvClass1Details;
    private TextView tvClass2Period, tvClass2Title, tvClass2Details;
    private TextView tvOcrRawText;

    private Uri selectedImageUri = null;
    private ActivityResultLauncher<String> imagePickerLauncher;

    private final List<Task> scannedTasks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_schedule);

        btnBack = findViewById(R.id.btnBack);
        imgSchedulePreview = findViewById(R.id.imgSchedulePreview);
        btnPickGallery = findViewById(R.id.btnPickGallery);
        btnAnalyzeSchedule = findViewById(R.id.btnAnalyzeSchedule);
        btnImportSchedule = findViewById(R.id.btnImportSchedule);

        edtStartDate = findViewById(R.id.edtStartDate);
        edtEndDate = findViewById(R.id.edtEndDate);

        tvSubjectCount = findViewById(R.id.tvSubjectCount);
        tvDateTitle = findViewById(R.id.tvDateTitle);

        tvClass1Period = findViewById(R.id.tvClass1Period);
        tvClass1Title = findViewById(R.id.tvClass1Title);
        tvClass1Details = findViewById(R.id.tvClass1Details);

        tvClass2Period = findViewById(R.id.tvClass2Period);
        tvClass2Title = findViewById(R.id.tvClass2Title);
        tvClass2Details = findViewById(R.id.tvClass2Details);

        tvOcrRawText = findViewById(R.id.tvOcrRawText);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar startCal = Calendar.getInstance();
        edtStartDate.setText(dateFormat.format(startCal.getTime()));

        Calendar endCal = Calendar.getInstance();
        endCal.add(Calendar.DAY_OF_MONTH, 60); // Mặc định áp dụng lịch học hằng ngày trong 60 ngày (~ 2 tháng)
        edtEndDate.setText(dateFormat.format(endCal.getTime()));

        if (edtStartDate != null) {
            edtStartDate.setFocusable(false);
            edtStartDate.setClickable(true);
            edtStartDate.setOnClickListener(v -> showDatePickerDialog(edtStartDate));
        }

        if (edtEndDate != null) {
            edtEndDate.setFocusable(false);
            edtEndDate.setClickable(true);
            edtEndDate.setOnClickListener(v -> showDatePickerDialog(edtEndDate));
        }

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN"));
        if (tvDateTitle != null) {
            tvDateTitle.setText(sdf.format(cal.getTime()));
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        imgSchedulePreview.setImageURI(uri);
                        imgSchedulePreview.setPadding(0, 0, 0, 0);
                        Toast.makeText(this, "Đã chọn ảnh! Nhấn 'Quét FlowAI' để đọc nội dung thực tế.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        if (btnPickGallery != null) {
            btnPickGallery.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        }

        if (btnAnalyzeSchedule != null) {
            btnAnalyzeSchedule.setOnClickListener(v -> analyzeImageWithMlKit());
        }

        if (btnImportSchedule != null) {
            btnImportSchedule.setOnClickListener(v -> importScannedScheduleToServer());
        }

        setupDefaultScannedTasks();
    }

    private void showDatePickerDialog(EditText targetEditText) {
        Calendar cal = Calendar.getInstance();

        String currentText = targetEditText.getText().toString().trim();
        if (currentText.contains("-")) {
            try {
                String[] parts = currentText.split("-");
                if (parts.length == 3) {
                    int year = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]) - 1;
                    int day = Integer.parseInt(parts[2]);
                    cal.set(year, month, day);
                }
            } catch (Exception ignored) {}
        }

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                ScanScheduleActivity.this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String selectedDateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                    targetEditText.setText(selectedDateStr);
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void analyzeImageWithMlKit() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Vui lòng chọn ảnh lịch học/làm việc từ bộ sưu tập trước!", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            InputImage image = InputImage.fromFilePath(this, selectedImageUri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            btnAnalyzeSchedule.setEnabled(false);
            btnAnalyzeSchedule.setText("Đang đọc...");

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        btnAnalyzeSchedule.setEnabled(true);
                        btnAnalyzeSchedule.setText("⚡ Quét FlowAI");

                        String resultText = visionText.getText();
                        if (resultText != null && !resultText.trim().isEmpty()) {
                            tvOcrRawText.setText(resultText);
                            parseOcrTextToClasses(resultText);
                            Toast.makeText(this, "✔ Quét thành công văn bản thực tế bằng FlowAI!", Toast.LENGTH_SHORT).show();
                        } else {
                            tvOcrRawText.setText("Không tìm thấy văn bản rõ ràng trong ảnh. Đang dùng cấu hình mẫu.");
                            Toast.makeText(this, "Không quét được chữ từ ảnh!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        btnAnalyzeSchedule.setEnabled(true);
                        btnAnalyzeSchedule.setText("⚡ Quét FlowAI");
                        Toast.makeText(this, "Lỗi đọc ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi đọc file ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void parseOcrTextToClasses(String rawText) {
        String[] lines = rawText.split("\n");
        List<String> textLines = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                textLines.add(trimmed);
            }
        }

        scannedTasks.clear();
        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", -1);

        if (textLines.size() >= 2) {
            String title1 = textLines.get(0);
            String desc1 = textLines.size() > 1 ? textLines.get(1) : "Trích xuất thực tế từ ảnh";

            if (tvClass1Title != null) tvClass1Title.setText(title1);
            if (tvClass1Details != null) tvClass1Details.setText(desc1);
            if (tvClass1Period != null) tvClass1Period.setText("1 - 3");

            Task task1 = new Task();
            task1.setTitle(title1 + " (Tiết 1-3)");
            task1.setDescription(desc1);
            task1.setTime("🕒 07:00 - 09:30");
            task1.setLocation("PMT 13/H");
            task1.setProjectId(2); // Category Học tập
            task1.setStatus("PENDING");
            task1.setUserId(currentUserId);
            scannedTasks.add(task1);

            if (textLines.size() >= 3) {
                String title2 = textLines.get(2);
                String desc2 = textLines.size() > 3 ? textLines.get(3) : "Trích xuất thực tế từ ảnh";

                if (tvClass2Title != null) tvClass2Title.setText(title2);
                if (tvClass2Details != null) tvClass2Details.setText(desc2);
                if (tvClass2Period != null) tvClass2Period.setText("9 - 12");

                Task task2 = new Task();
                task2.setTitle(title2 + " (Tiết 9-12)");
                task2.setDescription(desc2);
                task2.setTime("🕒 15:00 - 17:30");
                task2.setLocation("H.A9-403");
                task2.setProjectId(2); // Category Học tập
                task2.setStatus("PENDING");
                task2.setUserId(currentUserId);
                scannedTasks.add(task2);
            }

            if (tvSubjectCount != null) {
                tvSubjectCount.setText("● " + scannedTasks.size() + " Môn học");
            }
        } else {
            setupDefaultScannedTasks();
        }
    }

    private void setupDefaultScannedTasks() {
        scannedTasks.clear();
        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", -1);

        Task task1 = new Task();
        task1.setTitle("Thực hành Lập trình & Phát triển di động (Tiết 1-3)");
        task1.setDescription("Phòng máy tính 13/H • Giảng viên: Trần Thị Huệ");
        task1.setTime("🕒 07:00 - 09:30");
        task1.setLocation("PMT 13/H");
        task1.setProjectId(2);
        task1.setStatus("PENDING");
        task1.setUserId(currentUserId);
        scannedTasks.add(task1);

        Task task2 = new Task();
        task2.setTitle("Hệ điều hành nhúng (Tiết 9-12)");
        task2.setDescription("Phòng học H.A9-403 • Giảng viên: Ngô Quang Trí");
        task2.setTime("🕒 15:00 - 17:30");
        task2.setLocation("H.A9-403");
        task2.setProjectId(2);
        task2.setStatus("PENDING");
        task2.setUserId(currentUserId);
        scannedTasks.add(task2);
    }

    private void importScannedScheduleToServer() {
        if (scannedTasks.isEmpty()) {
            Toast.makeText(this, "Không có môn học nào để đồng bộ!", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", -1);

        if (currentUserId == -1) {
            Toast.makeText(this, "Lỗi: Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        String pStart = edtStartDate != null ? edtStartDate.getText().toString().trim() : "";
        String pEnd = edtEndDate != null ? edtEndDate.getText().toString().trim() : "";

        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();

        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            if (!pStart.isEmpty()) startCal.setTime(sdfDate.parse(pStart));
            if (!pEnd.isEmpty()) endCal.setTime(sdfDate.parse(pEnd));
        } catch (Exception ignored) {}

        // Sinh các buổi học lặp hằng ngày/hàng tuần cho toàn bộ học kỳ từ pStart đến pEnd
        List<Task> recurringTasks = new ArrayList<>();

        for (Task baseTask : scannedTasks) {
            Calendar loopCal = (Calendar) startCal.clone();

            while (!loopCal.after(endCal)) {
                String dateStr = sdfDate.format(loopCal.getTime());

                Task newTask = new Task();
                newTask.setTitle(baseTask.getTitle());
                newTask.setDescription(baseTask.getDescription());
                newTask.setTime("📅 " + dateStr + " | " + baseTask.getTime());
                newTask.setLocation(baseTask.getLocation());
                newTask.setProjectId(2); // Category "Học tập"
                newTask.setStatus("PENDING");
                newTask.setUserId(currentUserId);

                recurringTasks.add(newTask);

                // Lặp theo tuần (cách 7 ngày) cho lịch học môn này
                loopCal.add(Calendar.DAY_OF_MONTH, 7);
            }
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiService apiService = retrofit.create(ApiService.class);

        btnImportSchedule.setEnabled(false);
        btnImportSchedule.setText("Đang đồng bộ lịch học...");

        final int totalRecurring = recurringTasks.size();
        final int[] completedCount = new int[]{0};

        for (Task rTask : recurringTasks) {
            apiService.addTask(rTask).enqueue(new Callback<Task>() {
                @Override
                public void onResponse(Call<Task> call, Response<Task> response) {
                    checkAndFinishScheduleImport(completedCount, totalRecurring, pStart, pEnd);
                }

                @Override
                public void onFailure(Call<Task> call, Throwable t) {
                    checkAndFinishScheduleImport(completedCount, totalRecurring, pStart, pEnd);
                }
            });
        }
    }

    private synchronized void checkAndFinishScheduleImport(int[] completedCount, int totalRecurring, String pStart, String pEnd) {
        completedCount[0]++;
        if (completedCount[0] >= totalRecurring) {
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(ScanScheduleActivity.this, "✔ Đã tự động lặp " + totalRecurring + " buổi học hằng ngày từ " + pStart + " đến " + pEnd + "!", Toast.LENGTH_LONG).show();

                Intent intent = new Intent(ScanScheduleActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }
    }
}
