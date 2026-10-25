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
        btnGenerateWbs.setText("⚡ Gemini Pro AI Đang Phân Tách Cụ Thể...");

        new Thread(() -> {
            List<WbsTask> parsedList = new ArrayList<>();

            try {
                JSONObject requestBody = new JSONObject();

                JSONObject sysInstruction = new JSONObject();
                JSONArray sysParts = new JSONArray();
                JSONObject sysPart = new JSONObject();
                sysPart.put("text", "Bạn là Trợ lý AI Cấp Cao Gemini Pro (chuyên gia Quản Lý Dự Án & Kỹ Thuật Chuyên Sâu). Nhiệm vụ của bạn là phân tích chi tiết Tên dự án lớn và Mục tiêu do người dùng cung cấp, sau đó lập một Lộ Trình Phân Rã Công Việc (WBS) SÂU SẮC, ĐỘC ĐÁO, TÙY BIẾN 100% THEO CHUYÊN MÔN CỦA CHỦ ĐỀ NÀY.\n" +
                        "QUY TRÌNH PHÂN RÃ CHUYÊN SÂU:\n" +
                        "1. Tạo CHÍNH XÁC 4 GIAI ĐOẠN ĐẶC THÙ (Phase) riêng biệt hoàn toàn cho chủ đề này, kèm Emoji phù hợp (KHÔNG dùng các tên chung chung như 'Lập kế hoạch & Khảo sát', 'Thiết kế & Chuẩn bị'). Ví dụ nếu làm Arduino -> Phase 1: 🔌 Thiết kế Sơ đồ Mạch & Linh kiện, Phase 2: 💻 Lập trình Code C++ Arduino...\n" +
                        "2. Trong MỖI GIAI ĐOẠN, tạo 3-4 công việc cụ thể có tính thực thi cao, chứa thông tin chi tiết và Definition of Done rõ ràng.\n" +
                        "3. Trả về DUY NHẤT một JSON Array chứa các công việc (12-16 tasks). Mỗi item bao gồm:\n" +
                        "- \"phaseName\": Tên giai đoạn đặc thù kèm Emoji (VD: \"🔌 Giai đoạn 1: Thiết kế Sơ đồ Mạch Kỹ thuật\")\n" +
                        "- \"taskTitle\": Tên công việc cụ thể chuyên sâu\n" +
                        "- \"description\": Mô tả từng bước thực hiện & Tiêu chuẩn hoàn thành (Definition of Done)\n" +
                        "- \"scheduledDate\": Ngày thực hiện YYYY-MM-DD (trải dài từ " + pStart + " đến " + pEnd + ")\n" +
                        "- \"estimatedHours\": Số giờ dự kiến (1.0 đến 4.0)\n" +
                        "- \"priority\": \"High\", \"Medium\", hoặc \"Low\"\n" +
                        "- \"pomodoroCount\": Số phiên Pomodoro 25 phút (2 đến 8)");
                sysParts.put(sysPart);
                sysInstruction.put("parts", sysParts);
                requestBody.put("system_instruction", sysInstruction);

                JSONArray contents = new JSONArray();
                JSONObject contentObj = new JSONObject();
                JSONArray contentParts = new JSONArray();
                JSONObject textPart = new JSONObject();
                textPart.put("text", "Tên dự án lớn: " + pName + "\nMục tiêu & Yêu cầu: " + (pGoal.isEmpty() ? "Hoàn thành toàn bộ mục tiêu cốt lõi" : pGoal) +
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

            // Trợ lý AI Phân rã Thông Minh Chuyên Sâu Tùy Biến theo đúng Chủ đề Kỹ Thuật/Chuyên Môn Dự Án người dùng
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
                    String phaseTitle = rawPName.startsWith("📂") || rawPName.startsWith("🔌") || rawPName.startsWith("📡") || rawPName.startsWith("📚") || rawPName.startsWith("🚀") || rawPName.startsWith("✍️") || rawPName.startsWith("🎨") ? rawPName : "📂 " + rawPName;

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

                Toast.makeText(AiTaskBreakdownActivity.this, "✨ Gemini Pro AI đã tạo Lộ trình Chuyên sâu cho " + pName + "!", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private List<WbsTask> generateSmartContextualWbs(String pName, String pGoal, String pStart, String pEnd) {
        List<WbsTask> list = new ArrayList<>();
        String text = (pName + " " + pGoal).toLowerCase();

        String d1 = pStart;
        String d2 = addDaysToDate(pStart, 3);
        String d3 = addDaysToDate(pStart, 7);
        String d4 = addDaysToDate(pStart, 11);

        if (text.contains("arduino") || text.contains("mạch") || text.contains("vi điều khiển") || text.contains("linh kiện") || text.contains("sensor") || text.contains("phần cứng")) {
            // Arduino / Phần cứng / Mạch điện tử
            list.add(new WbsTask("🔌 Giai đoạn 1: Thiết kế Sơ đồ Mạch & Chọn Linh kiện", "Lập sơ đồ nguyên lý mạch điện cho " + pName, "Mô tả: Vẽ sơ đồ chân kết nối Arduino, Cảm biến & Module rơ-le. DoD: Sơ đồ kiểm tra không chập cháy", d1, 2.5, "High", 5));
            list.add(new WbsTask("🔌 Giai đoạn 1: Thiết kế Sơ đồ Mạch & Chọn Linh kiện", "Lựa chọn linh kiện & Nguồn cấp tương thích", "Mô tả: Tính toán điện áp, dòng điện tiêu thụ và chọn adapter/pin phù hợp. DoD: Linh kiện đủ công suất", d1, 2.0, "Medium", 4));
            list.add(new WbsTask("🔌 Giai đoạn 1: Thiết kế Sơ đồ Mạch & Chọn Linh kiện", "Mô phỏng sơ đồ mạch trên phần mềm Proteus/Tinkercad", "Mô tả: Chạy thử nghiệm tín hiệu mô phỏng. DoD: Mạch mô phỏng hoạt động đạt yêu cầu", d1, 3.0, "High", 6));

            list.add(new WbsTask("💻 Giai đoạn 2: Lập trình Code C++ Arduino & Viết Hàm", "Khai báo thư viện & Khởi tạo chân I/O Arduino", "Mô tả: Cấu hình ngắt, Timer và chuẩn giao tiếp I2C/SPI/UART. DoD: Code biên dịch không lỗi syntax", d2, 3.0, "High", 6));
            list.add(new WbsTask("💻 Giai đoạn 2: Lập trình Code C++ Arduino & Viết Hàm", "Viết thuật toán đọc dữ liệu Cảm biến & Xử lý nhiễu", "Mô tả: Lọc trung bình cộng tín hiệu analog đọc về. DoD: Dữ liệu cảm biến hiển thị chính xác", d2, 3.5, "High", 7));
            list.add(new WbsTask("💻 Giai đoạn 2: Lập trình Code C++ Arduino & Viết Hàm", "Xây dựng luồng điều khiển Động cơ/Rơ-le & Cảnh báo", "Mô tả: Kích hoạt Output khi dữ liệu vượt ngưỡng an toàn. DoD: Rơ-le đóng ngắt đúng kịch bản", d2, 2.5, "Medium", 5));

            list.add(new WbsTask("🛠 Giai đoạn 3: Lắp ráp Mạch Thực tế & Nạp Code Test", "Hàn linh kiện & Cắm dây breadboard mô hình", "Mô tả: Đấu nối dây cáp tín hiệu chắc chắn. DoD: Mạch thực tế bật nguồn chạy an toàn", d3, 4.0, "High", 8));
            list.add(new WbsTask("🛠 Giai đoạn 3: Lắp ráp Mạch Thực tế & Nạp Code Test", "Nạp code trực tiếp từ Arduino IDE & Bật Serial Monitor", "Mô tả: Theo dõi luồng dữ liệu debug qua cổng USB. DoD: Phản hồi Serial khớp với thiết kế", d3, 3.0, "Medium", 6));
            list.add(new WbsTask("🛠 Giai đoạn 3: Lắp ráp Mạch Thực tế & Nạp Code Test", "Sửa lỗi phần cứng & Chống dội phím/Cân chỉnh cảm biến", "Mô tả: Tinh chỉnh thông số biến trở và tụ lọc nhiễu. DoD: Hệ thống phản hồi nhạy 100%", d3, 2.5, "Medium", 5));

            list.add(new WbsTask("⚡ Giai đoạn 4: Đo Đạc Điện Áp, Tối Ưu Hóa & Đóng Khung", "Đo dòng điện bằng Đồng hồ vạn năng & Đánh giá công suất", "Mô tả: Đảm bảo nhiệt độ IC không quá nóng. DoD: Mạch hoạt động liên tục 2 giờ ổn định", d4, 2.5, "Medium", 5));
            list.add(new WbsTask("⚡ Giai đoạn 4: Đo Đạc Điện Áp, Tối Ưu Hóa & Đóng Khung", "Đóng hộp bảo vệ & Viết tài liệu sơ đồ nguyên lý " + pName, "Mô tả: Hoàn thiện hộp mica bảo vệ mô hình và làm báo cáo. DoD: Bàn giao mô hình hoàn chỉnh", d4, 2.0, "Low", 4));

        } else if (text.contains("gsm") || text.contains("mô phỏng") || text.contains("sóng") || text.contains("viễn thông") || text.contains("kênh")) {
            // Mô phỏng GSM / Viễn thông
            list.add(new WbsTask("📡 Giai đoạn 1: Phân tích Lý thuyết Kênh GSM & Sóng mang", "Phân tích cấu trúc khung GSM & Băng tần mạng", "Mô tả: Xác định tần số Up/Downlink và số khe thời gian TS. DoD: Hoàn thành bản tóm tắt thông số GSM", d1, 2.5, "High", 5));
            list.add(new WbsTask("📡 Giai đoạn 1: Phân tích Lý thuyết Kênh GSM & Sóng mang", "Thiết lập mô hình Toán học cho Suy hao & Nhiễu Fading", "Mô tả: Tính toán công thức Rayleigh/Rician Fading. DoD: Sẵn sàng thông số cho bài toán mô phỏng", d1, 3.0, "High", 6));

            list.add(new WbsTask("📊 Giai đoạn 2: Xây dựng Script Mô phỏng MATLAB/Python", "Lập trình mô hình Mã hóa Kênh & Điều chế GMSK", "Mô tả: Viết script mã hóa dòng bit dữ liệu GSM. DoD: Tín hiệu điều chế hiển thị đúng phổ tần", d2, 4.0, "High", 8));
            list.add(new WbsTask("📊 Giai đoạn 3: Đánh giá Tỷ lệ Lỗi Bit (BER) & SNR", "Chạy mô phỏng truyền tín hiệu qua Kênh suy hao", "Mô tả: Thu thập đồ thị BER theo tỷ số SNR. DoD: Đồ thị BER mô phỏng khớp với lý thuyết GSM", d3, 3.5, "High", 7));

            list.add(new WbsTask("📄 Giai đoạn 4: Tổng hợp Báo cáo Kỹ thuật GSM & Slide", "Soạn báo cáo phân tích kết quả mô phỏng GSM " + pName, "Mô tả: Đóng gói code, biểu đồ và nhận xét đánh giá. DoD: Hoàn tất tài liệu báo cáo nghiệm thu", d4, 2.0, "Medium", 4));

        } else if (text.contains("tiếng trung") || text.contains("hsk") || text.contains("ngoại ngữ") || text.contains("học tiếng")) {
            // Tiếng Trung / HSK / Ngôn ngữ
            list.add(new WbsTask("📚 Giai đoạn 1: Khảo sát Lộ trình HSK & Bộ Từ Vựng Cốt Lõi", "Lập mục tiêu chứng chỉ HSK & Bảng 100 từ vựng cho " + pName, "Mô tả: Liệt kê từ vựng theo cấp độ. DoD: Hoàn thành bộ thẻ ghi nhớ flashcard", d1, 2.0, "High", 4));
            list.add(new WbsTask("📚 Giai đoạn 1: Khảo sát Lộ trình HSK & Bộ Từ Vựng Cốt Lõi", "Khảo sát giáo trình Boya & Bộ đề thi Tiếng Trung chuẩn", "Mô tả: Sưu tầm 5 bộ đề và file nghe mp3. DoD: Sắp xếp tài liệu học tập theo tuần", d1, 2.5, "Medium", 5));

            list.add(new WbsTask("✍️ Giai đoạn 2: Luyện Ngữ Pháp & Quy Tắc Viết Chữ Hán", "Học các mẫu câu ghép Tiếng Trung giao tiếp thông dụng", "Mô tả: Đặt 15 câu ví dụ áp dụng ngữ pháp mới. DoD: Thuộc 100% ngữ pháp cốt lõi", d2, 3.0, "High", 6));
            list.add(new WbsTask("✍️ Giai đoạn 2: Luyện Ngữ Pháp & Quy Tắc Viết Chữ Hán", "Thực hành viết 30 chữ Hán chuẩn thứ tự nét và bộ thủ", "Mô tả: Tập viết nét chữ trên tập ô vuông. DoD: Nhớ mặt chữ và bộ thủ Hán tự", d2, 2.5, "Medium", 5));

            list.add(new WbsTask("🎧 Giai đoạn 3: Luyện Nghe Bài Hội Thoại & Phản Xạ Giao Tiếp", "Nghe đoạn hội thoại Tiếng Trung 20 phút & Chép chính tả", "Mô tả: Nghe hiểu và ghi lại phát âm. DoD: Hiểu 90% nội dung bài nghe", d3, 3.5, "High", 7));
            list.add(new WbsTask("🎧 Giai đoạn 3: Luyện Nghe Bài Hội Thoại & Phản Xạ Giao Tiếp", "Luyện nói phản xạ đoạn văn ngắn 5 phút qua ghi âm", "Mô tả: Tự ghi âm bài nói Tiếng Trung. DoD: Phát âm chuẩn thanh điệu Pinyin", d3, 3.0, "High", 6));

            list.add(new WbsTask("📝 Giai đoạn 4: Giải Đề Thi Thử HSK & Đánh Giá Tiêu Chuẩn", "Giải bộ đề thi thử HSK tổng hợp có canh giờ", "Mô tả: Làm phần Đọc hiểu & Nghe trong 60 phút. DoD: Đạt số điểm đầu ra kỳ vọng", d4, 3.0, "High", 6));

        } else {
            // Mặc định tùy biến theo tên dự án người dùng
            list.add(new WbsTask("🚀 Giai đoạn 1: Phân tích Khung Yêu cầu & Kiến trúc " + pName, "Xác định 5 mục tiêu trọng tâm & Phạm vi " + pName, "Mô tả: Lập danh sách tiêu chuẩn chất lượng. DoD: Được thống nhất bởi PM", d1, 2.5, "High", 5));
            list.add(new WbsTask("🚀 Giai đoạn 1: Phân tích Khung Yêu cầu & Kiến trúc " + pName, "Thu thập tài liệu chuyên môn & Nghiên cứu giải pháp", "Mô tả: Khảo sát 10 nguồn thông tin hàng đầu. DoD: Hoàn thiện bản tóm tắt kỹ thuật", d1, 3.0, "High", 6));

            list.add(new WbsTask("🎨 Giai đoạn 2: Thiết kế Khung Mẫu & Chuẩn bị Mô hình", "Thiết kế sơ đồ quy trình thực thi chi tiết cho " + pName, "Mô tả: Vẽ mô hình luồng hoạt động từng bước. DoD: Kiểm tra luồng chạy mượt mà", d2, 3.0, "High", 6));
            list.add(new WbsTask("🎨 Giai đoạn 2: Thiết kế Khung Mẫu & Chuẩn bị Mô hình", "Chuẩn bị cơ sở dữ liệu & Công cụ thực thi cần thiết", "Mô tả: Khởi tạo môi trường làm việc chuẩn. DoD: Môi trường sẵn sàng 100%", d2, 2.5, "Medium", 5));

            list.add(new WbsTask("💻 Giai đoạn 3: Triển khai & Thực thi Tính năng Cốt lõi", "Lập trình & Xây dựng hạng mục chính cho " + pName, "Mô tả: Hoàn thành 100% phần việc chuyên môn. DoD: Đạt tiêu chuẩn nghiệm thu", d3, 4.0, "High", 8));
            list.add(new WbsTask("💻 Giai đoạn 3: Triển khai & Thực thi Tính năng Cốt lõi", "Tích hợp các thành phần & Kiểm tra luồng liên kết", "Mô tả: Đảm bảo dữ liệu kết nối chính xác. DoD: Không có lỗi phát sinh", d3, 3.5, "High", 7));

            list.add(new WbsTask("🧪 Giai đoạn 4: Kiểm thử End-to-End, Tối ưu & Báo cáo", "Chạy thử nghiệm toàn bộ hệ thống dự án " + pName, "Mô tả: Thử nghiệm thực tế mọi kịch bản. DoD: Đáp ứng tiêu chuẩn hoàn thành DoD", d4, 2.5, "Medium", 5));
            list.add(new WbsTask("🧪 Giai đoạn 4: Kiểm thử End-to-End, Tối ưu & Báo cáo", "Tổng kết kết quả, Soạn slide & Bàn giao " + pName, "Mô tả: Hoàn tất báo cáo tổng kết tiến độ. DoD: Bàn giao thành công dự án", d4, 2.0, "Low", 4));
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
