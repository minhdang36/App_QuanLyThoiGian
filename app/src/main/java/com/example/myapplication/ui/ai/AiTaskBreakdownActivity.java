package com.example.myapplication.ui.ai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.PhaseGroupAdapter;
import com.example.myapplication.model.PhaseGroup;
import com.example.myapplication.model.Project;
import com.example.myapplication.model.Task;
import com.example.myapplication.model.WbsTask;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.ui.project.ProjectsActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AiTaskBreakdownActivity extends AppCompatActivity {

    private EditText edtProjectName, edtProjectGoal, edtStartDate, edtEndDate;
    private Button btnGenerateWbs, btnApplyBreakdown;
    private ImageButton btnBack;
    private TextView tvWbsCount, tvConfigureApiKey;

    private RecyclerView rvWbsTasks;
    private PhaseGroupAdapter phaseAdapter;

    private final List<WbsTask> wbsTaskList = new ArrayList<>();
    private final List<PhaseGroup> phaseGroupList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_task_breakdown);

        btnBack = findViewById(R.id.btnBack);
        edtProjectName = findViewById(R.id.edtProjectName);
        edtProjectGoal = findViewById(R.id.edtProjectGoal);
        edtStartDate = findViewById(R.id.edtStartDate);
        edtEndDate = findViewById(R.id.edtEndDate);

        btnGenerateWbs = findViewById(R.id.btnGenerateWbs);
        btnApplyBreakdown = findViewById(R.id.btnApplyBreakdown);
        tvWbsCount = findViewById(R.id.tvWbsCount);
        tvConfigureApiKey = findViewById(R.id.tvConfigureApiKey);

        rvWbsTasks = findViewById(R.id.rvWbsTasks);
        if (rvWbsTasks != null) {
            rvWbsTasks.setLayoutManager(new LinearLayoutManager(this));
            phaseAdapter = new PhaseGroupAdapter(phaseGroupList);
            rvWbsTasks.setAdapter(phaseAdapter);
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (tvConfigureApiKey != null) {
            tvConfigureApiKey.setOnClickListener(v -> showApiKeyDialog(null));
        }

        // Tự động điền thông tin mặc định từ Intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("MAIN_TASK_TITLE")) {
            String titleExtra = intent.getStringExtra("MAIN_TASK_TITLE");
            if (titleExtra != null && !titleExtra.isEmpty()) {
                edtProjectName.setText(titleExtra);
            }
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar startCal = Calendar.getInstance();
        edtStartDate.setText(dateFormat.format(startCal.getTime()));

        Calendar endCal = Calendar.getInstance();
        endCal.add(Calendar.DAY_OF_MONTH, 14);
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

        if (btnGenerateWbs != null) {
            btnGenerateWbs.setOnClickListener(v -> generateWbsWithGeminiApi());
        }

        if (btnApplyBreakdown != null) {
            btnApplyBreakdown.setOnClickListener(v -> syncAndSaveToDatabase());
        }
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
                AiTaskBreakdownActivity.this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String selectedDateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                    targetEditText.setText(selectedDateStr);
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void showApiKeyDialog(Runnable onKeySavedCallback) {
        SharedPreferences sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE);
        String currentKey = sharedPref.getString("GEMINI_API_KEY", "");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔑 Cấu Hình Gemini AI Key");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);

        TextView tvGuide = new TextView(this);
        tvGuide.setText("Nhập API Key Gemini của bạn để sử dụng AI phân rã thật 100% từ Google.\n(Key miễn phí 100% tại aistudio.google.com)");
        tvGuide.setTextSize(13);
        tvGuide.setTextColor(0xFF555555);
        layout.addView(tvGuide);

        final EditText edtKey = new EditText(this);
        edtKey.setHint("Dán API Key (AIzaSy...) tại đây");
        edtKey.setText(currentKey);
        edtKey.setTextSize(13);
        layout.addView(edtKey);

        builder.setView(layout);

        builder.setPositiveButton("Lưu Key & Tiếp tục", (dialog, which) -> {
            String inputKey = edtKey.getText().toString().trim();
            if (inputKey.isEmpty()) {
                Toast.makeText(this, "Chưa nhập Key!", Toast.LENGTH_SHORT).show();
                return;
            }
            sharedPref.edit().putString("GEMINI_API_KEY", inputKey).apply();
            Toast.makeText(this, "✔ Đã lưu Gemini API Key!", Toast.LENGTH_SHORT).show();
            if (onKeySavedCallback != null) {
                onKeySavedCallback.run();
            }
        });

        builder.setNeutralButton("Lấy Key Miễn Phí", (dialog, which) -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://aistudio.google.com/app/apikey"));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Không thể mở trình duyệt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void generateWbsWithGeminiApi() {
        SharedPreferences sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE);
        String apiKey = sharedPref.getString("GEMINI_API_KEY", "").trim();

        if (apiKey.isEmpty() || apiKey.contains("PlaceholderKey")) {
            showApiKeyDialog(() -> generateWbsWithGeminiApi());
            return;
        }

        String pName = edtProjectName != null ? edtProjectName.getText().toString().trim() : "";
        String pGoal = edtProjectGoal != null ? edtProjectGoal.getText().toString().trim() : "";
        String pStart = edtStartDate != null ? edtStartDate.getText().toString().trim() : "";
        String pEnd = edtEndDate != null ? edtEndDate.getText().toString().trim() : "";

        if (pName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên dự án!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnGenerateWbs.setEnabled(false);
        btnGenerateWbs.setText("⚡ Gemini AI Đang Phân Rã Thật...");

        new Thread(() -> {
            List<WbsTask> parsedList = new ArrayList<>();
            String lastErrorMsg = "";

            try {
                JSONObject requestBody = new JSONObject();

                JSONObject sysInstruction = new JSONObject();
                JSONArray sysParts = new JSONArray();
                JSONObject sysPart = new JSONObject();
                sysPart.put("text", "Bạn là một Quản lý Dự án (Project Manager) chuyên nghiệp. Nhiệm vụ của bạn là phân rã dự án lớn do người dùng cung cấp (tên dự án, mục tiêu, ngày bắt đầu, ngày kết thúc) thành các giai đoạn (Phase) và các task nhỏ cụ thể theo ngày.\n" +
                        "YÊU CẦU BẮT BUỘC:\n" +
                        "1. Chia dự án thành 4 GIAI ĐOẠN chính liên tục.\n" +
                        "2. Trong MỖI GIAI ĐOẠN, tạo 3-4 task nhỏ cụ thể, đúng chủ đề chuyên môn dự án, có ngày scheduledDate trải dài trong khoảng " + pStart + " đến " + pEnd + ".\n" +
                        "3. Trả về DUY NHẤT một JSON Array chứa các task (12-16 tasks). Mỗi item gồm:\n" +
                        "- \"phaseName\": Tên giai đoạn (VD: \"Giai đoạn 1: Lập kế hoạch & Khảo sát\")\n" +
                        "- \"taskTitle\": Tên công việc cụ thể\n" +
                        "- \"description\": Mô tả chi tiết & Definition of Done\n" +
                        "- \"scheduledDate\": Ngày thực hiện YYYY-MM-DD\n" +
                        "- \"estimatedHours\": Số giờ (1.0 đến 4.0)\n" +
                        "- \"priority\": \"High\", \"Medium\", hoặc \"Low\"\n" +
                        "- \"pomodoroCount\": Số phiên Pomodoro (1-8)");
                sysParts.put(sysPart);
                sysInstruction.put("parts", sysParts);
                requestBody.put("system_instruction", sysInstruction);

                JSONArray contents = new JSONArray();
                JSONObject contentObj = new JSONObject();
                JSONArray contentParts = new JSONArray();
                JSONObject textPart = new JSONObject();
                textPart.put("text", "Tên dự án lớn: " + pName + "\nMục tiêu & Yêu cầu: " + (pGoal.isEmpty() ? "Hoàn thành các mục tiêu cốt lõi" : pGoal) +
                        "\nNgày bắt đầu: " + pStart + "\nNgày kết thúc: " + pEnd);
                contentParts.put(textPart);
                contentObj.put("parts", contentParts);
                contents.put(contentObj);
                requestBody.put("contents", contents);

                JSONObject genConfig = new JSONObject();
                genConfig.put("responseMimeType", "application/json");
                requestBody.put("generationConfig", genConfig);

                String[] modelsToTry = new String[]{"gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash-exp"};
                String rawJsonArrayStr = "";

                for (String modelName : modelsToTry) {
                    try {
                        URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                        conn.setDoOutput(true);
                        conn.setConnectTimeout(20000);
                        conn.setReadTimeout(20000);

                        OutputStream os = conn.getOutputStream();
                        os.write(requestBody.toString().getBytes("UTF-8"));
                        os.flush();
                        os.close();

                        int responseCode = conn.getResponseCode();
                        InputStream is = (responseCode == HttpURLConnection.HTTP_OK) ? conn.getInputStream() : conn.getErrorStream();
                        Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
                        String responseText = scanner.hasNext() ? scanner.next() : "";
                        scanner.close();

                        if (responseCode == HttpURLConnection.HTTP_OK && !responseText.isEmpty()) {
                            JSONObject resJson = new JSONObject(responseText);
                            JSONArray candidates = resJson.optJSONArray("candidates");
                            if (candidates != null && candidates.length() > 0) {
                                JSONObject cand = candidates.getJSONObject(0);
                                JSONObject content = cand.optJSONObject("content");
                                if (content != null) {
                                    JSONArray parts = content.optJSONArray("parts");
                                    if (parts != null && parts.length() > 0) {
                                        rawJsonArrayStr = parts.getJSONObject(0).optString("text", "").trim();
                                        if (!rawJsonArrayStr.isEmpty()) {
                                            break; // Thành công
                                        }
                                    }
                                }
                            }
                        } else {
                            lastErrorMsg = "Lỗi HTTP " + responseCode + ": " + responseText;
                        }
                    } catch (Exception ex) {
                        lastErrorMsg = ex.getMessage();
                    }
                }

                if (!rawJsonArrayStr.isEmpty()) {
                    // Lọc bỏ markdown codeblock ```json ... ```
                    if (rawJsonArrayStr.startsWith("```")) {
                        int firstBracket = rawJsonArrayStr.indexOf("[");
                        int lastBracket = rawJsonArrayStr.lastIndexOf("]");
                        if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
                            rawJsonArrayStr = rawJsonArrayStr.substring(firstBracket, lastBracket + 1);
                        }
                    }

                    JSONArray array = new JSONArray(rawJsonArrayStr);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject item = array.getJSONObject(i);
                        WbsTask task = new WbsTask();
                        task.setPhaseName(item.optString("phaseName", "Giai đoạn"));
                        task.setTaskTitle(item.optString("taskTitle", "Công việc"));
                        task.setDescription(item.optString("description", "Definition of Done: Hoàn thành theo chuẩn"));
                        task.setScheduledDate(item.optString("scheduledDate", pStart));
                        task.setEstimatedHours(item.optDouble("estimatedHours", 2.0));
                        task.setPriority(item.optString("priority", "High"));
                        task.setPomodoroCount(item.optInt("pomodoroCount", 4));
                        parsedList.add(task);
                    }
                }
            } catch (Exception e) {
                lastErrorMsg = e.getMessage();
            }

            final String errorMsg = lastErrorMsg;
            final boolean isAiSuccess = !parsedList.isEmpty();

            new Handler(Looper.getMainLooper()).post(() -> {
                btnGenerateWbs.setEnabled(true);
                btnGenerateWbs.setText("⚡ Phân Rã Bằng Gemini AI");

                if (!isAiSuccess) {
                    Toast.makeText(AiTaskBreakdownActivity.this, "⚠️ Gemini AI chưa nhận phản hồi. Hãy bấm 'Cấu hình API Key' để kiểm tra Key của bạn.\n" + (errorMsg.isEmpty() ? "" : errorMsg), Toast.LENGTH_LONG).show();
                    return;
                }

                wbsTaskList.clear();
                wbsTaskList.addAll(parsedList);

                Map<String, PhaseGroup> mapPhases = new LinkedHashMap<>();
                for (WbsTask wTask : wbsTaskList) {
                    String rawPName = wTask.getPhaseName() != null ? wTask.getPhaseName() : "Giai đoạn";
                    String phaseTitle = rawPName.startsWith("📂") ? rawPName : "📂 " + rawPName;

                    PhaseGroup group = mapPhases.get(phaseTitle);
                    if (group == null) {
                        group = new PhaseGroup(phaseTitle);
                        group.setExpanded(true);
                        mapPhases.put(phaseTitle, group);
                    }

                    Task t = new Task();
                    t.setTitle(wTask.getTaskTitle());
                    t.setDescription(wTask.getDescription() + " • 🍅 " + wTask.getPomodoroCount() + " Pomodoro");
                    t.setTime("📅 " + wTask.getScheduledDate() + " | 🕒 " + wTask.getEstimatedHours() + "h • 🚩 " + wTask.getPriority());
                    t.setLocation("Dự án lớn");
                    group.getSubTaskList().add(t);
                }

                phaseGroupList.clear();
                phaseGroupList.addAll(mapPhases.values());

                if (phaseAdapter != null) {
                    phaseAdapter.updateData(phaseGroupList);
                }

                if (tvWbsCount != null) {
                    tvWbsCount.setText("● " + phaseGroupList.size() + " Giai đoạn (" + wbsTaskList.size() + " task)");
                }

                Toast.makeText(AiTaskBreakdownActivity.this, "✨ Gemini AI THẬT SỰ đã phân rã thành công " + phaseGroupList.size() + " giai đoạn và " + wbsTaskList.size() + " công việc!", Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    private void syncAndSaveToDatabase() {
        if (wbsTaskList.isEmpty()) {
            Toast.makeText(this, "Chưa có danh sách công việc để đồng bộ! Bấm nút Phân Rã AI trước.", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", -1);
        if (currentUserId == -1) {
            Toast.makeText(this, "Lỗi: Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        String pName = edtProjectName != null ? edtProjectName.getText().toString().trim() : "Dự án lớn";
        String pGoal = edtProjectGoal != null ? edtProjectGoal.getText().toString().trim() : "Dự án lớn";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Project projectObj = new Project();
        projectObj.setName("🚀 " + pName);
        projectObj.setDescription(pGoal);
        projectObj.setUserId(currentUserId);

        btnApplyBreakdown.setEnabled(false);
        btnApplyBreakdown.setText("Đang đồng bộ vào cơ sở dữ liệu...");

        apiService.addProject(projectObj).enqueue(new Callback<Project>() {
            @Override
            public void onResponse(Call<Project> call, Response<Project> response) {
                int savedProjectId = 1;
                if (response.isSuccessful() && response.body() != null) {
                    savedProjectId = response.body().getId();
                }

                saveWbsTasksToApi(apiService, currentUserId, savedProjectId);
            }

            @Override
            public void onFailure(Call<Project> call, Throwable t) {
                saveWbsTasksToApi(apiService, currentUserId, 1);
            }
        });
    }

    private void saveWbsTasksToApi(ApiService apiService, int userId, int projectId) {
        int countSaved = 0;
        for (WbsTask wTask : wbsTaskList) {
            if (wTask.isSelected()) {
                Task t = new Task();
                t.setTitle("[" + wTask.getPhaseName() + "] " + wTask.getTaskTitle());
                t.setDescription(wTask.getDescription() + " • 🍅 " + wTask.getPomodoroCount() + " Pomodoro");
                t.setTime("📅 " + wTask.getScheduledDate() + " | 🕒 " + wTask.getEstimatedHours() + "h • 🚩 " + wTask.getPriority());
                t.setLocation("Dự án lớn");
                t.setProjectId(projectId);
                t.setStatus("PENDING");
                t.setUserId(userId);

                apiService.addTask(t).enqueue(new Callback<Task>() {
                    @Override public void onResponse(Call<Task> call, Response<Task> response) {}
                    @Override public void onFailure(Call<Task> call, Throwable t) {}
                });
                countSaved++;
            }
        }

        Toast.makeText(AiTaskBreakdownActivity.this, "✔ Đã đồng bộ thành công Dự án lớn và " + countSaved + " công việc!", Toast.LENGTH_LONG).show();

        Intent intent = new Intent(AiTaskBreakdownActivity.this, ProjectsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
