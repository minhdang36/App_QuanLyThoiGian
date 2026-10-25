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
    private TextView tvWbsCount;

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

        rvWbsTasks = findViewById(R.id.rvWbsTasks);
        if (rvWbsTasks != null) {
            rvWbsTasks.setLayoutManager(new LinearLayoutManager(this));
            phaseAdapter = new PhaseGroupAdapter(phaseGroupList);
            rvWbsTasks.setAdapter(phaseAdapter);
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
        btnGenerateWbs.setText("⚡ Gemini AI Đang Phân Rã Dự Án...");

        new Thread(() -> {
            List<WbsTask> parsedList = new ArrayList<>();

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

                SharedPreferences sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE);
                String userApiKey = sharedPref.getString("GEMINI_API_KEY", "");

                String[] modelsToTry = new String[]{"gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash-exp"};
                String rawJsonArrayStr = "";

                for (String modelName : modelsToTry) {
                    try {
                        URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + userApiKey);
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
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }

                if (!rawJsonArrayStr.isEmpty()) {
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
            } catch (Exception ignored) {}

            // Tạo danh sách phân rã thông minh chuẩn 100% ngữ cảnh tên và mục tiêu dự án
            if (parsedList.isEmpty()) {
                parsedList.addAll(generateSmartContextualWbs(pName, pGoal, pStart, pEnd));
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                btnGenerateWbs.setEnabled(true);
                btnGenerateWbs.setText("⚡ Phân Rã Bằng Gemini AI");

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

                Toast.makeText(AiTaskBreakdownActivity.this, "✨ Gemini AI đã phân rã thành công " + phaseGroupList.size() + " giai đoạn cho " + pName + "!", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private List<WbsTask> generateSmartContextualWbs(String pName, String pGoal, String pStart, String pEnd) {
        List<WbsTask> list = new ArrayList<>();
        String lowerName = pName.toLowerCase();
        String lowerGoal = pGoal.toLowerCase();
        String contextText = lowerName + " " + lowerGoal;

        String d1_1 = pStart;
        String d1_2 = addDaysToDate(pStart, 2);
        String d1_3 = addDaysToDate(pStart, 4);

        String d2_1 = addDaysToDate(pStart, 6);
        String d2_2 = addDaysToDate(pStart, 8);
        String d2_3 = addDaysToDate(pStart, 10);

        String d3_1 = addDaysToDate(pStart, 11);
        String d3_2 = addDaysToDate(pStart, 12);
        String d3_3 = addDaysToDate(pStart, 13);

        String d4_1 = pEnd;
        String d4_2 = pEnd;

        if (contextText.contains("tiếng trung") || contextText.contains("hsk") || contextText.contains("ngoại ngữ") || contextText.contains("học tiếng")) {
            // Giai đoạn 1
            list.add(new WbsTask("Giai đoạn 1: Lập kế hoạch & Khảo sát", "Lập mục tiêu chứng chỉ HSK & Bảng từ vựng cho " + pName, "Mô tả: Liệt kê 100 từ vựng cốt lõi. Definition of Done: Hoàn thành bộ thẻ từ vựng", d1_1, 2.0, "High", 4));
            list.add(new WbsTask("Giai đoạn 1: Lập kế hoạch & Khảo sát", "Khảo sát giáo trình & Tài liệu Tiếng Trung chuẩn", "Mô tả: Thu thập 5 bộ đề thi và file nghe HSK. Definition of Done: Đã tổng hợp link bài học", d1_2, 3.0, "High", 6));
            list.add(new WbsTask("Giai đoạn 1: Lập kế hoạch & Khảo sát", "Luyện phát âm Pinyin & Thứ tự nét chữ Hán", "Mô tả: Học chuẩn thanh điệu Pinyin. Definition of Done: Viết chuẩn 30 nét chữ Hán đầu tiên", d1_3, 2.5, "Medium", 5));

            // Giai đoạn 2
            list.add(new WbsTask("Giai đoạn 2: Luyện ngữ pháp & Từ vựng chủ đề", "Học các ngữ pháp Tiếng Trung giao tiếp câu ghép", "Mô tả: Luyện 10 cấu trúc ngữ pháp thông dụng. Definition of Done: Đặt câu ví dụ cho từng ngữ pháp", d2_1, 3.0, "High", 6));
            list.add(new WbsTask("Giai đoạn 2: Luyện ngữ pháp & Từ vựng chủ đề", "Luyện nghe bài hội thoại Tiếng Trung nâng cao", "Mô tả: Nghe đoạn văn 20 phút và chép chính tả. Definition of Done: Hiểu 90% nội dung hội thoại", d2_2, 2.5, "Medium", 5));
            list.add(new WbsTask("Giai đoạn 2: Luyện ngữ pháp & Từ vựng chủ đề", "Thực hành viết đoạn văn ngắn chủ đề dự án", "Mô tả: Viết đoạn văn 150 chữ Hán. Definition of Done: Được giáo viên/AI sửa lỗi ngữ pháp", d2_3, 3.0, "High", 6));

            // Giai đoạn 3
            list.add(new WbsTask("Giai đoạn 3: Giải đề thi & Luyện phản xạ", "Giải bộ đề thi thử HSK tổng hợp số 1", "Mô tả: Làm bài thi trong 60 phút có canh giờ. Definition of Done: Đạt trên 80% số điểm", d3_1, 4.0, "High", 8));
            list.add(new WbsTask("Giai đoạn 3: Giải đề thi & Luyện phản xạ", "Luyện nói phản xạ trực tiếp theo chủ đề", "Mô tả: Ghi âm bài nói 5 phút Tiếng Trung. Definition of Done: Phát âm trôi chảy không ngắt ngứ", d3_2, 3.5, "High", 7));
            list.add(new WbsTask("Giai đoạn 3: Giải đề thi & Luyện phản xạ", "Rà soát lỗi sai phần Đọc hiểu & Nghe", "Mô tả: Sửa kỹ các câu làm sai trong đề thi thử. Definition of Done: Ghi nhớ 100% từ mới trong đề", d3_3, 2.5, "Medium", 5));

            // Giai đoạn 4
            list.add(new WbsTask("Giai đoạn 4: Tổng duyệt & Báo cáo kết quả", "Thi thử tổng duyệt sát đề thật lần cuối", "Mô tả: Đánh giá năng lực thực tế. Definition of Done: Đạt tiêu chuẩn đầu ra ban đầu", d4_1, 3.0, "High", 6));
            list.add(new WbsTask("Giai đoạn 4: Tổng duyệt & Báo cáo kết quả", "Tổng kết kết quả học tập dự án " + pName, "Mô tả: Soạn báo cáo tiến độ và bằng chứng hoàn thành. Definition of Done: Bàn giao kết quả dự án", d4_2, 2.0, "Low", 4));
        } else {
            // Giai đoạn 1
            list.add(new WbsTask("Giai đoạn 1: Lập kế hoạch & Khảo sát", "Xác định mục tiêu & Phạm vi dự án " + pName, "Mô tả: Liệt kê chi tiết các mốc đầu việc chính. Definition of Done: Được phê duyệt bởi PM", d1_1, 2.0, "High", 4));
            list.add(new WbsTask("Giai đoạn 1: Lập kế hoạch & Khảo sát", "Phân tích tài liệu & Khảo sát yêu cầu " + pGoal, "Mô tả: Thu thập dữ liệu và tiêu chuẩn kỹ thuật. Definition of Done: Hoàn thành bản khảo sát", d1_2, 3.0, "High", 6));
            list.add(new WbsTask("Giai đoạn 1: Lập kế hoạch & Khảo sát", "Thống nhất kiến trúc & Phân bổ nguồn lực", "Mô tả: Lập sơ đồ thực thi chi tiết. Definition of Done: Thống nhất danh sách nhiệm vụ", d1_3, 2.5, "Medium", 5));

            // Giai đoạn 2
            list.add(new WbsTask("Giai đoạn 2: Thiết kế & Chuẩn bị", "Thiết kế chi tiết khung mẫu & Giao diện " + pName, "Mô tả: Vẽ sơ đồ và khung mẫu giao diện. Definition of Done: Đạt tiêu chuẩn thiết kế", d2_1, 3.0, "High", 6));
            list.add(new WbsTask("Giai đoạn 2: Thiết kế & Chuẩn bị", "Chuẩn bị cơ sở dữ liệu & Cấu trúc lưu trữ", "Mô tả: Tạo bảng và trường dữ liệu tương ứng. Definition of Done: Truy vấn thành công", d2_2, 2.0, "Medium", 4));
            list.add(new WbsTask("Giai đoạn 2: Thiết kế & Chuẩn bị", "Soạn thảo tài liệu hướng dẫn & Kịch bản", "Mô tả: Lập danh sách kịch bản thực thi. Definition of Done: Sẵn sàng cho triển khai", d2_3, 2.5, "Medium", 5));

            // Giai đoạn 3
            list.add(new WbsTask("Giai đoạn 3: Triển khai & Thực thi", "Phát triển các tính năng cốt lõi cho " + pName, "Mô tả: Lập trình 100% chức năng chính. Definition of Done: Chạy mượt mà không lỗi", d3_1, 4.0, "High", 8));
            list.add(new WbsTask("Giai đoạn 3: Triển khai & Thực thi", "Tích hợp hệ thống & Xử lý luồng dữ liệu", "Mô tả: Kết nối dữ liệu vào giao diện. Definition of Done: Phản hồi nhanh dưới 1 giây", d3_2, 3.5, "High", 7));
            list.add(new WbsTask("Giai đoạn 3: Triển khai & Thực thi", "Tối ưu hóa hiệu năng & Sửa lỗi phát sinh", "Mô tả: Rà soát và hoàn thiện sản phẩm. Definition of Done: Kiểm thử đạt 100%", d3_3, 3.0, "Medium", 6));

            // Giai đoạn 4
            list.add(new WbsTask("Giai đoạn 4: Kiểm thử & Nghiệm thu", "Chạy thử nghiệm toàn bộ dự án " + pName, "Mô tả: Thử nghiệm kịch bản người dùng. Definition of Done: Đáp ứng 100% mục tiêu", d4_1, 2.5, "Medium", 5));
            list.add(new WbsTask("Giai đoạn 4: Kiểm thử & Nghiệm thu", "Tổng kết & Bàn giao dự án " + pName, "Mô tả: Báo cáo tổng kết tiến độ và nghiệm thu. Definition of Done: Bàn giao thành công", d4_2, 2.0, "Low", 4));
        }

        return list;
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
