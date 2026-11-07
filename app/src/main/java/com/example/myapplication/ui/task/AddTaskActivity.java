package com.example.myapplication.ui.task;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.model.Project;
import com.example.myapplication.model.Task;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.ui.ai.AiTaskBreakdownActivity;
import com.example.myapplication.ui.main.MainActivity;
import com.example.myapplication.ui.project.AddProjectActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddTaskActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO = 101;

    private EditText edtTaskTitle, edtDescription, edtLocation;
    private TextView tvSelectDate, tvSelectTime, tvPriority, tvCancel;
    private TextView tvVoiceStatus, tvVoiceTimer, tvTranscribedText;
    private TextView btnVoiceClear, btnVoiceApply;
    private ImageButton btnBack, btnQuickMic, btnVoiceToggle;
    private Button btnSave;
    private Spinner spinnerCategory;

    private TextView chipCatWork, chipCatStudy, chipCatPersonal, chipCatAdd;

    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    private int voiceTimerSeconds = 0;
    private final Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    private List<Project> currentProjects;
    private int taskId = -1;
    private int initialProjectId = -1;
    private String initialStatus = "PENDING";
    private int selectedChipIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        btnBack = findViewById(R.id.btnBack);
        edtTaskTitle = findViewById(R.id.edtTaskTitle);
        edtDescription = findViewById(R.id.edtDescription);
        edtLocation = findViewById(R.id.edtLocation);
        btnQuickMic = findViewById(R.id.btnQuickMic);

        chipCatWork = findViewById(R.id.chipCatWork);
        chipCatStudy = findViewById(R.id.chipCatStudy);
        chipCatPersonal = findViewById(R.id.chipCatPersonal);
        chipCatAdd = findViewById(R.id.chipCatAdd);

        spinnerCategory = findViewById(R.id.spinnerCategory);
        tvSelectDate = findViewById(R.id.tvSelectDate);
        tvSelectTime = findViewById(R.id.tvSelectTime);
        tvPriority = findViewById(R.id.tvPriority);

        tvVoiceStatus = findViewById(R.id.tvVoiceStatus);
        tvVoiceTimer = findViewById(R.id.tvVoiceTimer);
        tvTranscribedText = findViewById(R.id.tvTranscribedText);
        btnVoiceClear = findViewById(R.id.btnVoiceClear);
        btnVoiceToggle = findViewById(R.id.btnVoiceToggle);
        btnVoiceApply = findViewById(R.id.btnVoiceApply);

        tvCancel = findViewById(R.id.tvCancel);
        btnSave = findViewById(R.id.btnSave);

        Calendar today = Calendar.getInstance();
        int currentDay = today.get(Calendar.DAY_OF_MONTH);
        int currentMonth = today.get(Calendar.MONTH) + 1;
        int currentYear = today.get(Calendar.YEAR);
        String currentDateStr = "📅 " + currentDay + "/" + currentMonth + "/" + currentYear;
        tvSelectDate.setText(currentDateStr);

        int currentHour = today.get(Calendar.HOUR_OF_DAY);
        int currentMinute = today.get(Calendar.MINUTE);
        String currentTimeStr = String.format(Locale.getDefault(), "🕒 %02d:%02d", currentHour, currentMinute);
        tvSelectTime.setText(currentTimeStr);

        btnBack.setOnClickListener(v -> finish());
        tvCancel.setOnClickListener(v -> finish());

        ImageButton btnAiAssist = findViewById(R.id.btnAiAssist);
        if (btnAiAssist != null) {
            btnAiAssist.setOnClickListener(v -> {
                Intent intent = new Intent(AddTaskActivity.this, AiTaskBreakdownActivity.class);
                startActivity(intent);
            });
        }

        taskId = getIntent().getIntExtra("TASK_ID", -1);
        initialProjectId = getIntent().getIntExtra("PROJECT_ID", -1);
        String statusExtra = getIntent().getStringExtra("TASK_STATUS");
        if (statusExtra != null && !statusExtra.isEmpty()) {
            initialStatus = statusExtra;
        }

        if (taskId != -1) {
            edtTaskTitle.setText(getIntent().getStringExtra("TASK_TITLE"));
            edtDescription.setText(getIntent().getStringExtra("TASK_DESC"));
            edtLocation.setText(getIntent().getStringExtra("TASK_LOC"));

            String taskTime = getIntent().getStringExtra("TASK_TIME");
            if (taskTime != null && taskTime.contains("|")) {
                String[] timeParts = taskTime.split("\\|");
                if (timeParts.length >= 2) {
                    tvSelectDate.setText(timeParts[0].trim());
                    String rightPart = timeParts[1].trim();
                    if (rightPart.contains("•")) {
                        String[] subParts = rightPart.split("•");
                        tvSelectTime.setText(subParts[0].trim());
                        applyPriorityText(subParts[1].trim());
                    } else {
                        tvSelectTime.setText(rightPart);
                    }
                }
            } else if (taskTime != null && !taskTime.isEmpty()) {
                tvSelectDate.setText(taskTime);
            }
            btnSave.setText("Cập nhật công việc");
        }

        setupCategoryChips();
        setupPickers();
        setupSpeechRecognition();

        btnSave.setOnClickListener(v -> saveTaskData());
    }

    private void applyPriorityText(String rawPriority) {
        if (tvPriority == null || rawPriority == null) return;
        String upper = rawPriority.toUpperCase();
        if (upper.contains("2") || upper.contains("MEDIUM") || upper.contains("TRUNG BÌNH")) {
            tvPriority.setText("🚩 Ưu 2");
            tvPriority.setTextColor(0xFFFF9800);
        } else if (upper.contains("3") || upper.contains("LOW") || upper.contains("THẤP")) {
            tvPriority.setText("🚩 Ưu 3");
            tvPriority.setTextColor(0xFF4CAF50);
        } else {
            tvPriority.setText("🚩 Ưu 1");
            tvPriority.setTextColor(0xFFE53935);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCategoriesForSpinner();
    }

    private void setupCategoryChips() {
        chipCatWork.setOnClickListener(v -> selectCategoryChip(chipCatWork, 0));
        chipCatStudy.setOnClickListener(v -> selectCategoryChip(chipCatStudy, 1));
        chipCatPersonal.setOnClickListener(v -> selectCategoryChip(chipCatPersonal, 2));

        chipCatAdd.setOnClickListener(v -> {
            Intent intent = new Intent(AddTaskActivity.this, AddProjectActivity.class);
            startActivity(intent);
        });
    }

    private void selectCategoryChip(TextView selectedChip, int position) {
        selectedChipIndex = position;
        int inactiveChipText = ContextCompat.getColor(this, R.color.brand_primary);
        chipCatWork.setBackgroundResource(R.drawable.bg_pill_light);
        chipCatWork.setTextColor(inactiveChipText);
        chipCatStudy.setBackgroundResource(R.drawable.bg_pill_light);
        chipCatStudy.setTextColor(inactiveChipText);
        chipCatPersonal.setBackgroundResource(R.drawable.bg_pill_light);
        chipCatPersonal.setTextColor(inactiveChipText);

        selectedChip.setBackgroundResource(R.drawable.bg_pill_brown);
        selectedChip.setTextColor(0xFFFFFFFF);

        if (spinnerCategory.getAdapter() != null && spinnerCategory.getAdapter().getCount() > position) {
            spinnerCategory.setSelection(position);
        }
    }

    private void setupPickers() {
        tvSelectDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(AddTaskActivity.this,
                    (view, selectedYear, monthOfYear, dayOfMonth) -> {
                        String selectedDate = "📅 " + dayOfMonth + "/" + (monthOfYear + 1) + "/" + selectedYear;
                        tvSelectDate.setText(selectedDate);
                    }, year, month, day);
            datePickerDialog.show();
        });

        tvSelectTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int pickerHour = calendar.get(Calendar.HOUR_OF_DAY);
            int pickerMinute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(AddTaskActivity.this,
                    (view, hourOfDay, minute1) -> {
                        String formattedTime = String.format(Locale.getDefault(), "🕒 %02d:%02d", hourOfDay, minute1);
                        tvSelectTime.setText(formattedTime);
                    }, pickerHour, pickerMinute, true);
            timePickerDialog.show();
        });

        tvPriority.setOnClickListener(v -> {
            if (tvPriority.getText().toString().contains("1")) {
                tvPriority.setText("🚩 Ưu 2");
                tvPriority.setTextColor(0xFFFF9800);
            } else if (tvPriority.getText().toString().contains("2")) {
                tvPriority.setText("🚩 Ưu 3");
                tvPriority.setTextColor(0xFF4CAF50);
            } else {
                tvPriority.setText("🚩 Ưu 1");
                tvPriority.setTextColor(0xFFE53935);
            }
        });
    }

    private void setupSpeechRecognition() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    tvVoiceStatus.setText("● ĐANG LẮNG NGHE...");
                }

                @Override
                public void onBeginningOfSpeech() {}

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {}

                @Override
                public void onError(int error) {
                    tvVoiceStatus.setText("● TẠM DỪNG");
                    stopVoiceTimer();
                    isListening = false;
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String voiceText = matches.get(0);
                        tvTranscribedText.setText("“" + voiceText + "”");
                    }
                    tvVoiceStatus.setText("● HOÀN THÀNH");
                    stopVoiceTimer();
                    isListening = false;
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        tvTranscribedText.setText("“" + matches.get(0) + "”");
                    }
                }

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        }

        View.OnClickListener startVoiceClick = v -> toggleVoiceRecognition();
        btnQuickMic.setOnClickListener(startVoiceClick);
        btnVoiceToggle.setOnClickListener(startVoiceClick);

        btnVoiceClear.setOnClickListener(v -> {
            tvTranscribedText.setText("“Chưa có nội dung ghi âm...”");
            Toast.makeText(AddTaskActivity.this, "Đã xóa văn bản ghi âm", Toast.LENGTH_SHORT).show();
        });

        btnVoiceApply.setOnClickListener(v -> {
            String transcribed = tvTranscribedText.getText().toString().replace("“", "").replace("”", "").trim();
            if (!transcribed.isEmpty() && !transcribed.contains("Chưa có nội dung")) {
                if (edtTaskTitle.getText().toString().trim().isEmpty()) {
                    edtTaskTitle.setText(transcribed);
                } else {
                    edtDescription.setText(transcribed);
                }
                Toast.makeText(AddTaskActivity.this, "Đã áp dụng văn bản giọng nói!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AddTaskActivity.this, "Không có văn bản để áp dụng!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleVoiceRecognition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            return;
        }

        if (isListening) {
            if (speechRecognizer != null) speechRecognizer.stopListening();
            tvVoiceStatus.setText("● TẠM DỪNG");
            stopVoiceTimer();
            isListening = false;
            btnVoiceToggle.setImageResource(android.R.drawable.ic_btn_speak_now);
        } else {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN");
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

            if (speechRecognizer != null) speechRecognizer.startListening(intent);
            tvVoiceStatus.setText("● ĐANG LẮNG NGHE...");
            startVoiceTimer();
            isListening = true;
            btnVoiceToggle.setImageResource(android.R.drawable.ic_media_pause);
        }
    }

    private void startVoiceTimer() {
        voiceTimerSeconds = 0;
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                voiceTimerSeconds++;
                int minutes = voiceTimerSeconds / 60;
                int secs = voiceTimerSeconds % 60;
                tvVoiceTimer.setText(String.format(Locale.getDefault(), "⏱ %02d:%02d", minutes, secs));
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void stopVoiceTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            toggleVoiceRecognition();
        }
    }

    private void bindProjectsToSpinner(List<Project> projects) {
        currentProjects = projects;
        List<String> projectNames = new ArrayList<>(projects.size());
        int matchedIndex = -1;

        for (int i = 0; i < currentProjects.size(); i++) {
            Project p = currentProjects.get(i);
            projectNames.add(p.getName());
            if (initialProjectId != -1 && p.getId() == initialProjectId) {
                matchedIndex = i;
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                AddTaskActivity.this,
                android.R.layout.simple_spinner_dropdown_item,
                projectNames
        );
        spinnerCategory.setAdapter(adapter);

        if (matchedIndex != -1) {
            spinnerCategory.setSelection(matchedIndex);
        } else if (currentProjects.size() > selectedChipIndex) {
            spinnerCategory.setSelection(selectedChipIndex);
        }
    }

    private void loadCategoriesForSpinner() {
        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", -1);
        if (currentUserId == -1) return;

        List<Project> cached = RetrofitClient.getCachedProjects();
        if (!cached.isEmpty() && (currentProjects == null || currentProjects.isEmpty())) {
            bindProjectsToSpinner(cached);
        }

        ApiService apiService = RetrofitClient.getApiService();

        apiService.getProjectsByUser(currentUserId).enqueue(new Callback<List<Project>>() {
            @Override
            public void onResponse(Call<List<Project>> call, Response<List<Project>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Project> body = response.body();
                    RetrofitClient.setCachedProjects(body);
                    bindProjectsToSpinner(body);
                }
            }

            @Override
            public void onFailure(Call<List<Project>> call, Throwable t) {}
        });
    }

    private void saveTaskData() {
        String title = edtTaskTitle.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();
        String location = edtLocation.getText().toString().trim();

        String date = tvSelectDate.getText().toString().trim();
        String timeStr = tvSelectTime.getText().toString().trim();
        String selectedPriority = tvPriority != null ? tvPriority.getText().toString().trim() : "🚩 Ưu 1";
        String fullDateTime = date + " | " + timeStr + " • " + selectedPriority;

        if (title.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tiêu đề công việc!", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", -1);

        if (currentUserId == -1) {
            Toast.makeText(this, "Lỗi: Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedProjectId = initialProjectId != -1 ? initialProjectId : 1;
        String selectedCategoryName = "Công việc";
        if (currentProjects != null && !currentProjects.isEmpty()) {
            int selectedPosition = spinnerCategory.getSelectedItemPosition();
            if (selectedPosition >= 0 && selectedPosition < currentProjects.size()) {
                selectedProjectId = currentProjects.get(selectedPosition).getId();
                selectedCategoryName = currentProjects.get(selectedPosition).getName();
            } else if (selectedChipIndex >= 0 && selectedChipIndex < currentProjects.size()) {
                selectedProjectId = currentProjects.get(selectedChipIndex).getId();
                selectedCategoryName = currentProjects.get(selectedChipIndex).getName();
            } else {
                selectedProjectId = currentProjects.get(0).getId();
                selectedCategoryName = currentProjects.get(0).getName();
            }
        }

        Task taskData = new Task();
        taskData.setTitle(title);
        taskData.setDescription(description);
        taskData.setLocation(location);
        taskData.setTime(fullDateTime);
        taskData.setPriority(selectedPriority);
        taskData.setCategory(selectedCategoryName);
        taskData.setProjectId(selectedProjectId);
        taskData.setStatus(initialStatus);
        taskData.setUserId(currentUserId);

        ApiService apiService = RetrofitClient.getApiService();
        Call<Task> call = (taskId != -1) ? apiService.updateTask(taskId, taskData) : apiService.addTask(taskData);

        btnSave.setEnabled(false);
        btnSave.setText("Đang lưu...");

        call.enqueue(new Callback<Task>() {
            @Override
            public void onResponse(Call<Task> call, Response<Task> response) {
                btnSave.setEnabled(true);
                btnSave.setText(taskId != -1 ? "Cập nhật công việc" : "Lưu công việc");

                if (response.isSuccessful()) {
                    RetrofitClient.invalidateCache();
                    Toast.makeText(AddTaskActivity.this, "✔ Đã lưu công việc thành công!", Toast.LENGTH_SHORT).show();
                    if (taskId != -1) {
                        finish();
                    } else {
                        Intent intent = new Intent(AddTaskActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    Toast.makeText(AddTaskActivity.this, "⚠️ Lỗi khi lưu công việc (Mã: " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Task> call, Throwable t) {
                btnSave.setEnabled(true);
                btnSave.setText(taskId != -1 ? "Cập nhật công việc" : "Lưu công việc");
                Toast.makeText(AddTaskActivity.this, "❌ Không thể kết nối máy chủ! Vui lòng kiểm tra Server.", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        stopVoiceTimer();
        super.onDestroy();
    }
}
