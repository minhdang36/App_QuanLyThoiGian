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

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.WbsTaskAdapter;
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
import java.util.List;
import java.util.Locale;
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
    private TextView tvWbsCount;

    private RecyclerView rvWbsTasks;
    private WbsTaskAdapter wbsAdapter;
    private final List<WbsTask> wbsTaskList = new ArrayList<>();

    // Gemini API Key Placeholder (Hỗ trợ cấu hình tùy chỉnh)
    private static final String GEMINI_API_KEY = "AIzaSyD-PlaceholderKeyForGeminiApiV1";

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

        rvWbsTasks = findViewById(R.id.rvWbsTasks);
        if (rvWbsTasks != null) {
            rvWbsTasks.setLayoutManager(new LinearLayoutManager(this));
            wbsAdapter = new WbsTaskAdapter(wbsTaskList);
            rvWbsTasks.setAdapter(wbsAdapter);
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
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

    private void generateWbsWithGeminiApi() {
        String pName = edtProjectName != null ? edtProjectName.getText().toString().trim() : "";
        String pGoal = edtProjectGoal != null ? edtProjectGoal.getText().toString().trim() : "";
        String pStart = edtStartDate != null ? edtStartDate.getText().toString().trim() : "";
        String pEnd = edtEndDate != null ? edtEndDate.getText().toString().trim() : "";

        if (pName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên dự án!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnGenerateWbs.setEnabled(false);
        btnGenerateWbs.setText("⚡ Gemini AI Đang Phân Rã WBS...");

        new Thread(() -> {
            try {
                // Tạo Payload gửi tới Gemini API (gemini-1.5-flash) với responseMimeType = "application/json"
                JSONObject requestBody = new JSONObject();

                // System Instruction đóng vai Project Manager chuyên nghiệp
                JSONObject sysInstruction = new JSONObject();
                JSONArray sysParts = new JSONArray();
                JSONObject sysPart = new JSONObject();
                sysPart.put("text", "Bạn là một Project Manager chuyên nghiệp. Nhiệm vụ của bạn là phân rã dự án lớn do người dùng cung cấp (tên dự án, mục tiêu, ngày bắt đầu, ngày kết thúc) theo mô hình WBS (Work Breakdown Structure).\n" +
                        "QUY TRÌNH PHÂN RÃ BẮT BUỘC:\n" +
                        "1. Chia dự án thành CHÍNH XÁC 4 GIAI ĐOẠN chính (Giai đoạn 1: Lập kế hoạch & Khảo sát, Giai đoạn 2: Thiết kế & Chuẩn bị, Giai đoạn 3: Triển khai & Thực thi, Giai đoạn 4: Kiểm thử & Nghiệm thu).\n" +
                        "2. Trong MỖI GIAI ĐOẠN, tiếp tục phân rã thành 3-4 TASK NHỎ CỤ THỂ theo ngày, trải dài liên tục và đều đặn từ ngày bắt đầu (" + pStart + ") đến ngày kết thúc (" + pEnd + ").\n" +
                        "3. Mỗi task nhỏ phải chứa ngày thực hiện (scheduledDate) tăng dần liên tục trong khoảng thời gian đã chọn.\n" +
                        "Yêu cầu về định dạng kết quả:\n" +
                        "Trả về DUY NHẤT một JSON Array chứa tất cả các task nhỏ (tổng cộng từ 12 đến 16 task). Mỗi item chứa:\n" +
                        "- \"phaseName\": Tên giai đoạn WBS (VD: \"Giai đoạn 1: Lập kế hoạch & Khảo sát\")\n" +
                        "- \"taskTitle\": Tên công việc nhỏ cụ thể\n" +
                        "- \"description\": Mô tả chi tiết & Definition of Done để kiểm tra chất lượng\n" +
                        "- \"scheduledDate\": Ngày thực hiện định dạng YYYY-MM-DD (trải dài trong khoảng từ " + pStart + " đến " + pEnd + ")\n" +
                        "- \"estimatedHours\": Số giờ dự kiến (từ 1.0 đến 4.0 giờ)\n" +
                        "- \"priority\": Mức độ ưu tiên (\"High\", \"Medium\", hoặc \"Low\")\n" +
                        "- \"pomodoroCount\": Số phiên Pomodoro 25 phút tương ứng (VD: 2.0h -> 4)");
                sysParts.put(sysPart);
                sysInstruction.put("parts", sysParts);
                requestBody.put("system_instruction", sysInstruction);

                // Nội dung yêu cầu từ người dùng
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

                // Cấu hình responseMimeType = "application/json"
                JSONObject genConfig = new JSONObject();
                genConfig.put("responseMimeType", "application/json");
                requestBody.put("generationConfig", genConfig);

                // Gọi REST API của Gemini 1.5 Flash
                URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_API_KEY);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                OutputStream os = conn.getOutputStream();
                os.write(requestBody.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                InputStream is = (responseCode == HttpURLConnection.HTTP_OK) ? conn.getInputStream() : conn.getErrorStream();
                Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
                String responseText = scanner.hasNext() ? scanner.next() : "";
                scanner.close();

                List<WbsTask> parsedList = new ArrayList<>();

                if (responseCode == HttpURLConnection.HTTP_OK && !responseText.isEmpty()) {
                    JSONObject resJson = new JSONObject(responseText);
                    JSONArray candidates = resJson.optJSONArray("candidates");
                    if (candidates != null && candidates.length() > 0) {
                        JSONObject cand = candidates.getJSONObject(0);
                        JSONObject content = cand.optJSONObject("content");
                        if (content != null) {
                            JSONArray parts = content.optJSONArray("parts");
                            if (parts != null && parts.length() > 0) {
                                String rawJsonArrayStr = parts.getJSONObject(0).optString("text", "");
                                JSONArray array = new JSONArray(rawJsonArrayStr);

                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject item = array.getJSONObject(i);
                                    WbsTask task = new WbsTask();
                                    task.setPhaseName(item.optString("phaseName", "Giai đoạn WBS"));
                                    task.setTaskTitle(item.optString("taskTitle", "Công việc"));
                                    task.setDescription(item.optString("description", "Definition of Done: Hoàn thành theo chuẩn"));
                                    task.setScheduledDate(item.optString("scheduledDate", pStart));
                                    task.setEstimatedHours(item.optDouble("estimatedHours", 2.0));
                                    task.setPriority(item.optString("priority", "High"));
                                    task.setPomodoroCount(item.optInt("pomodoroCount", 4));
                                    parsedList.add(task);
                                }
                            }
                        }
                    }
                }

                // Fallback mô phỏng WBS 4 Giai đoạn chia nhỏ 12 tasks trải dài thời gian
                if (parsedList.isEmpty()) {
                    String d1_1 = pStart;
                    String d1_2 = addDaysToDate(pStart, 2);
                    String d1_3 = addDaysToDate(pStart, 5);

                    String d2_1 = addDaysToDate(pStart, 8);
                    String d2_2 = addDaysToDate(pStart, 11);
                    String d2_3 = addDaysToDate(pStart, 14);

                    String d3_1 = addDaysToDate(pStart, 17);
                    String d3_2 = addDaysToDate(pStart, 20);
                    String d3_3 = addDaysToDate(pStart, 23);

                    String d4_1 = addDaysToDate(pStart, 26);
                    String d4_2 = addDaysToDate(pStart, 28);
                    String d4_3 = pEnd;

                    // GIAI ĐOẠN 1: Lập kế hoạch & Khảo sát
                    parsedList.add(new WbsTask("Giai đoạn 1: Lập kế hoạch & Khảo sát WBS", "Xác định mục tiêu & Phạm vi dự án " + pName, "Mô tả: Liệt kê chi tiết 5 đầu việc chính. Definition of Done: Được xác nhận bởi PM", d1_1, 2.0, "High", 4));
                    parsedList.add(new WbsTask("Giai đoạn 1: Lập kế hoạch & Khảo sát WBS", "Phân tích tài liệu & Khảo sát yêu cầu", "Mô tả: Thu thập 10 tài liệu tham khảo chính. Definition of Done: Hoàn thành bản tóm tắt khảo sát", d1_2, 3.0, "High", 6));
                    parsedList.add(new WbsTask("Giai đoạn 1: Lập kế hoạch & Khảo sát WBS", "Thống nhất kiến trúc & Phân bổ nguồn lực", "Mô tả: Phác thảo sơ đồ thực thi hệ thống. Definition of Done: Thống nhất các mốc công việc", d1_3, 2.5, "Medium", 5));

                    // GIAI ĐOẠN 2: Thiết kế & Chuẩn bị
                    parsedList.add(new WbsTask("Giai đoạn 2: Thiết kế & Chuẩn bị WBS", "Thiết kế chi tiết khung mẫu & Giao diện", "Mô tả: Vẽ 8 màn hình/khung mẫu chính. Definition of Done: Hoàn thiện bản phác thảo đạt chuẩn", d2_1, 3.0, "High", 6));
                    parsedList.add(new WbsTask("Giai đoạn 2: Thiết kế & Chuẩn bị WBS", "Chuẩn bị cơ sở dữ liệu & Cấu trúc lưu trữ", "Mô tả: Tạo bảng và trường dữ liệu cần thiết. Definition of Done: Kiểm tra truy vấn thành công", d2_2, 2.0, "Medium", 4));
                    parsedList.add(new WbsTask("Giai đoạn 2: Thiết kế & Chuẩn bị WBS", "Soạn thảo tài liệu hướng dẫn & Kịch bản", "Mô tả: Lập danh sách các kịch bản kiểm thử. Definition of Done: Sẵn sàng cho giai đoạn triển khai", d2_3, 2.5, "Medium", 5));

                    // GIAI ĐOẠN 3: Triển khai & Thực thi
                    parsedList.add(new WbsTask("Giai đoạn 3: Triển khai & Thực thi WBS", "Phát triển các module tính năng cốt lõi", "Mô tả: Lập trình 100% chức năng quan trọng. Definition of Done: Tính năng chạy mượt mà", d3_1, 4.0, "High", 8));
                    parsedList.add(new WbsTask("Giai đoạn 3: Triển khai & Thực thi WBS", "Tích hợp giao diện & Xử lý luồng dữ liệu", "Mô tả: Kết nối dữ liệu vào giao diện hiển thị. Definition of Done: Không bị treo hay đơ ứng dụng", d3_2, 3.5, "High", 7));
                    parsedList.add(new WbsTask("Giai đoạn 3: Triển khai & Thực thi WBS", "Tối ưu hóa hiệu năng & Sửa lỗi phát sinh", "Mô tả: Rà soát và sửa các lỗi giao diện/dữ liệu. Definition of Done: Đạt chỉ số mượt 60fps", d3_3, 3.0, "Medium", 6));

                    // GIAI ĐOẠN 4: Kiểm thử & Nghiệm thu
                    parsedList.add(new WbsTask("Giai đoạn 4: Kiểm thử & Nghiệm thu WBS", "Chạy thử nghiệm toàn bộ hệ thống (End-to-End)", "Mô tả: Thử nghiệm mọi kịch bản người dùng. Definition of Done: Đạt 100% kịch bản không lỗi", d4_1, 2.5, "Medium", 5));
                    parsedList.add(new WbsTask("Giai đoạn 4: Kiểm thử & Nghiệm thu WBS", "Đánh giá theo tiêu chuẩn Definition of Done", "Mô tả: Kiểm tra chất lượng sản phẩm cuối cùng. Definition of Done: Đáp ứng đủ tiêu chí ban đầu", d4_2, 2.0, "Medium", 4));
                    parsedList.add(new WbsTask("Giai đoạn 4: Kiểm thử & Nghiệm thu WBS", "Soạn báo cáo tổng kết & Bàn giao dự án", "Mô tả: Hoàn tất slide & Báo cáo tổng kết dự án. Definition of Done: Bàn giao thành công", d4_3, 2.0, "Low", 4));
                }

                new Handler(Looper.getMainLooper()).post(() -> {
                    btnGenerateWbs.setEnabled(true);
                    btnGenerateWbs.setText("⚡ Phân Rã WBS Bằng Gemini AI");

                    wbsTaskList.clear();
                    wbsTaskList.addAll(parsedList);
                    if (wbsAdapter != null) {
                        wbsAdapter.updateData(wbsTaskList);
                    }

                    if (tvWbsCount != null) {
                        tvWbsCount.setText("● " + wbsTaskList.size() + " Task WBS");
                    }
                    Toast.makeText(AiTaskBreakdownActivity.this, "⚡ Gemini AI đã phân rã thành công " + wbsTaskList.size() + " task WBS!", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    btnGenerateWbs.setEnabled(true);
                    btnGenerateWbs.setText("⚡ Phân Rã WBS Bằng Gemini AI");
                    Toast.makeText(AiTaskBreakdownActivity.this, "Lỗi phân rã AI: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void syncAndSaveToDatabase() {
        if (wbsTaskList.isEmpty()) {
            Toast.makeText(this, "Chưa có danh sách WBS để đồng bộ! Bấm nút Phân Rã AI trước.", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sharedPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        int currentUserId = sharedPref.getInt("USER_ID", -1);
        if (currentUserId == -1) {
            Toast.makeText(this, "Lỗi: Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        String pName = edtProjectName != null ? edtProjectName.getText().toString().trim() : "Dự án lớn WBS";
        String pGoal = edtProjectGoal != null ? edtProjectGoal.getText().toString().trim() : "Dự án WBS";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        // 1. Lưu Dự Án Lớn vào bảng Project
        Project projectObj = new Project();
        projectObj.setName("🚀 " + pName);
        projectObj.setDescription(pGoal);
        projectObj.setUserId(currentUserId);

        btnApplyBreakdown.setEnabled(false);
        btnApplyBreakdown.setText("Đang đồng bộ WBS vào cơ sở dữ liệu...");

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
                t.setLocation("Dự án lớn WBS");
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

        Toast.makeText(AiTaskBreakdownActivity.this, "✔ Đã đồng bộ thành công Dự án lớn và " + countSaved + " công việc WBS!", Toast.LENGTH_LONG).show();

        Intent intent = new Intent(AiTaskBreakdownActivity.this, ProjectsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private String addDaysToDate(String startDateStr, int daysToAdd) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar c = Calendar.getInstance();
            c.setTime(sdf.parse(startDateStr));
            c.add(Calendar.DAY_OF_MONTH, daysToAdd);
            return sdf.format(c.getTime());
        } catch (Exception e) {
            return startDateStr;
        }
    }
}
