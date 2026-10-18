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
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

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
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    private List<Project> currentProjects;
    private int taskId = -1;
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
        String currentDateStr = "📅 Hôm nay, " + currentDay + "/" + currentMonth;
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
        if (taskId != -1) {
            edtTaskTitle.setText(getIntent().getStringExtra("TASK_TITLE"));
            edtDescription.setText(getIntent().getStringExtra("TASK_DESC"));
            edtLocation.setText(getIntent().getStringExtra("TASK_LOC"));

            String taskTime = getIntent().getStringExtra("TASK_TIME");
            if (taskTime != null && taskTime.contains("|")) {
                String[] timeParts = taskTime.split("\\|");
                if (timeParts.length >= 2) {
                    tvSelectDate.setText(timeParts[0].trim());
                    tvSelectTime.setText(timeParts[1].trim());
                }
            } else if (taskTime != null && !taskTime.isEmpty()) {
                tvSelectDate.setText(taskTime);
            }
            btnSave.setText("Cập nhật công việc");
        }

        loadCategoriesForSpinner();

        setupCategoryChips();
        setupPickers();
        setupSpeechRecognition();

        btnSave.setOnClickListener(v -> saveTaskData());
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
        chipCatWork.setBackgroundResource(R.drawable.bg_pill_light);
        chipCatWork.setTextColor(0xFF555555);
        chipCatStudy.setBackgroundResource(R.drawable.bg_pill_light);
        chipCatStudy.setTextColor(0xFF555555);
        chipCatPersonal.setBackgroundResource(R.drawable.bg_pill_light);
        chipCatPersonal.setTextColor(0xFF555555);

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
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            toggleVoiceRecognition();
        }
    }

    private void loadCategoriesForSpinner() {
        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", -1);
        if (currentUserId == -1) return;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiService apiService = retrofit.create(ApiService.class);

        apiService.getProjectsByUser(currentUserId).enqueue(new Callback<List<Project>>() {
            @Override
            public void onResponse(Call<List<Project>> call, Response<List<Project>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentProjects = response.body();
                    List<String> projectNames = new ArrayList<>();
                    for (Project p : currentProjects) {
                        projectNames.add(p.getName());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            AddTaskActivity.this,
                            android.R.layout.simple_spinner_dropdown_item,
                            projectNames
                    );
                    spinnerCategory.setAdapter(adapter);

                    if (currentProjects.size() > selectedChipIndex) {
                        spinnerCategory.setSelection(selectedChipIndex);
                    }
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

        int selectedProjectId = 1;
        if (currentProjects != null && !currentProjects.isEmpty()) {
            int selectedPosition = spinnerCategory.getSelectedItemPosition();
            if (selectedPosition >= 0 && selectedPosition < currentProjects.size()) {
                selectedProjectId = currentProjects.get(selectedPosition).getId();
            } else if (selectedChipIndex >= 0 && selectedChipIndex < currentProjects.size()) {
                selectedProjectId = currentProjects.get(selectedChipIndex).getId();
            } else {
                selectedProjectId = currentProjects.get(0).getId();
            }
        }

        Task taskData = new Task();
        taskData.setTitle(title);
        taskData.setDescription(description);
        taskData.setLocation(location);
        taskData.setTime(fullDateTime);
        taskData.setProjectId(selectedProjectId);
        taskData.setStatus("PENDING");
        taskData.setUserId(currentUserId);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        Call<Task> call = (taskId != -1) ? apiService.updateTask(taskId, taskData) : apiService.addTask(taskData);

        btnSave.setEnabled(false);
        btnSave.setText("Đang lưu...");

        call.enqueue(new Callback<Task>() {
            @Override
            public void onResponse(Call<Task> call, Response<Task> response) {
                Toast.makeText(AddTaskActivity.this, "✔ Đã lưu công việc thành công!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(AddTaskActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Call<Task> call, Throwable t) {
                Toast.makeText(AddTaskActivity.this, "✔ Đã lưu công việc thành công!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(AddTaskActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
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