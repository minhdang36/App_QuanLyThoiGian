package com.example.myapplication.ui.phase;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.model.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class PhaseDetailActivity extends AppCompatActivity {

    private static final String DEFAULT_GEMINI_API_KEY = "DÁN_API_KEY_CỦA_BẠN_VÀO_ĐÂY";

    private TextView tvPhaseDetailTitle, tvPhaseTaskCount, tvConfigureApiKey;
    private Button btnAiDecomposeTasks;
    private ImageButton btnBack;
    private LinearLayout layoutTasksContainer;

    private String phaseName = "📂 Giai đoạn 1: Lập kế hoạch & Khảo sát";
    private final List<Task> taskList = new ArrayList<>();
    private final Map<Integer, List<String>> taskSubTasksMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phase_detail);

        btnBack = findViewById(R.id.btnBack);
        tvPhaseDetailTitle = findViewById(R.id.tvPhaseDetailTitle);
        tvPhaseTaskCount = findViewById(R.id.tvPhaseTaskCount);
        tvConfigureApiKey = findViewById(R.id.tvConfigureApiKey);
        btnAiDecomposeTasks = findViewById(R.id.btnAiDecomposeTasks);
        layoutTasksContainer = findViewById(R.id.layoutTasksContainer);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (tvConfigureApiKey != null) tvConfigureApiKey.setOnClickListener(v -> showApiKeyDialog(null));

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("PHASE_NAME")) {
                String pName = intent.getStringExtra("PHASE_NAME");
                if (pName != null && !pName.trim().isEmpty()) {
                    phaseName = pName;
                }
            }

            if (intent.hasExtra("SUB_TASKS_JSON")) {
                String json = intent.getStringExtra("SUB_TASKS_JSON");
                if (json != null && !json.trim().isEmpty()) {
                    try {
                        Gson gson = new Gson();
                        Type listType = new TypeToken<ArrayList<Task>>() {}.getType();
                        List<Task> parsedList = gson.fromJson(json, listType);
                        if (parsedList != null) {
                            taskList.addAll(parsedList);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        if (tvPhaseDetailTitle != null) {
            tvPhaseDetailTitle.setText(phaseName);
        }

        if (tvPhaseTaskCount != null) {
            tvPhaseTaskCount.setText("● " + taskList.size() + " Công việc");
        }

        renderTaskCards();

        if (btnAiDecomposeTasks != null) {
            btnAiDecomposeTasks.setOnClickListener(v -> decomposeTasksWithGeminiAi());
        }
    }

    private void renderTaskCards() {
        if (layoutTasksContainer == null) return;
        layoutTasksContainer.removeAllViews();

        for (int i = 0; i < taskList.size(); i++) {
            Task task = taskList.get(i);

            MaterialCardView cardView = new MaterialCardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 32);
            cardView.setLayoutParams(cardParams);
            cardView.setCardBackgroundColor(0xFFFFFFFF);
            cardView.setRadius(40);
            cardView.setCardElevation(0);

            LinearLayout cardContent = new LinearLayout(this);
            cardContent.setOrientation(LinearLayout.VERTICAL);
            cardContent.setPadding(40, 40, 40, 40);

            TextView tvTitle = new TextView(this);
            tvTitle.setText("📌 " + (task.getTitle() != null ? task.getTitle() : "Công việc"));
            tvTitle.setTextColor(0xFF222222);
            tvTitle.setTextSize(16);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            cardContent.addView(tvTitle);

            if (task.getTime() != null && !task.getTime().isEmpty()) {
                TextView tvTime = new TextView(this);
                tvTime.setText(task.getTime());
                tvTime.setTextColor(0xFF10B981);
                tvTime.setTextSize(12);
                tvTime.setPadding(0, 8, 0, 0);
                cardContent.addView(tvTime);
            }

            if (task.getDescription() != null && !task.getDescription().isEmpty()) {
                TextView tvDesc = new TextView(this);
                tvDesc.setText(task.getDescription());
                tvDesc.setTextColor(0xFF666666);
                tvDesc.setTextSize(13);
                tvDesc.setPadding(0, 12, 0, 0);
                cardContent.addView(tvDesc);
            }

            LinearLayout layoutChecklist = new LinearLayout(this);
            layoutChecklist.setOrientation(LinearLayout.VERTICAL);
            layoutChecklist.setTag("checklist_" + i);
            layoutChecklist.setPadding(0, 16, 0, 0);

            List<String> subTasks = taskSubTasksMap.get(i);
            if (subTasks != null && !subTasks.isEmpty()) {
                TextView tvHeaderChecklist = new TextView(this);
                tvHeaderChecklist.setText("📋 Các đầu việc cần làm (Gemini AI đã phân tách):");
                tvHeaderChecklist.setTextColor(0xFF362C2A);
                tvHeaderChecklist.setTextSize(12);
                tvHeaderChecklist.setTypeface(null, android.graphics.Typeface.BOLD);
                tvHeaderChecklist.setPadding(0, 8, 0, 12);
                layoutChecklist.addView(tvHeaderChecklist);

                for (String st : subTasks) {
                    CheckBox checkBox = new CheckBox(this);
                    checkBox.setText(st);
                    checkBox.setTextColor(0xFF333333);
                    checkBox.setTextSize(13);
                    layoutChecklist.addView(checkBox);
                }
            } else {
                TextView tvHint = new TextView(this);
                tvHint.setText("💡 Bấm nút 'AI Phân Tách' ở trên để Gemini AI đào sâu phân tách danh sách đầu việc chi tiết cho công việc này.");
                tvHint.setTextColor(0xFF9E9E9E);
                tvHint.setTextSize(11);
                tvHint.setPadding(0, 8, 0, 0);
                layoutChecklist.addView(tvHint);
            }

            cardContent.addView(layoutChecklist);
            cardView.addView(cardContent);
            layoutTasksContainer.addView(cardView);
        }
    }

    private String getValidApiKey() {
        SharedPreferences sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE);
        String key = sharedPref.getString("GEMINI_API_KEY", "").trim();
        if (key.isEmpty() || key.contains("PlaceholderKey")) {
            return DEFAULT_GEMINI_API_KEY;
        }
        return key;
    }

    private void showApiKeyDialog(Runnable onKeySavedCallback) {
        String activeKey = getValidApiKey();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔑 Cấu Hình Gemini AI Key");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);

        TextView tvGuide = new TextView(this);
        tvGuide.setText("Nhập API Key Gemini của bạn để sử dụng AI phân rã thật 100% từ Google.\n(Lấy Key miễn phí 100% tại aistudio.google.com)");
        tvGuide.setTextSize(13);
        tvGuide.setTextColor(0xFF555555);
        layout.addView(tvGuide);

        final EditText edtKey = new EditText(this);
        edtKey.setHint("Dán API Key (AIzaSy...) tại đây");
        edtKey.setText(activeKey);
        edtKey.setTextSize(13);
        layout.addView(edtKey);

        builder.setView(layout);

        builder.setPositiveButton("Lưu Key & Tiếp tục", (dialog, which) -> {
            String inputKey = edtKey.getText().toString().trim();
            if (inputKey.isEmpty()) {
                Toast.makeText(this, "Chưa nhập Key!", Toast.LENGTH_SHORT).show();
                return;
            }
            getSharedPreferences("AppSettings", MODE_PRIVATE).edit().putString("GEMINI_API_KEY", inputKey).apply();
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

    private String discoverBestModelName(String apiKey) {
        String bestModel = null;
        try {
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                InputStream is = conn.getInputStream();
                Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
                String responseText = scanner.hasNext() ? scanner.next() : "";
                scanner.close();

                if (!responseText.isEmpty()) {
                    JSONObject resJson = new JSONObject(responseText);
                    JSONArray modelsArray = resJson.optJSONArray("models");
                    if (modelsArray != null) {
                        List<String> validModels = new ArrayList<>();
                        for (int i = 0; i < modelsArray.length(); i++) {
                            JSONObject mObj = modelsArray.getJSONObject(i);
                            String mName = mObj.optString("name", "");
                            JSONArray methods = mObj.optJSONArray("supportedGenerationMethods");

                            boolean supportsGenerate = false;
                            if (methods != null) {
                                for (int j = 0; j < methods.length(); j++) {
                                    if ("generateContent".equals(methods.optString(j))) {
                                        supportsGenerate = true;
                                        break;
                                    }
                                }
                            }

                            if (supportsGenerate && mName.toLowerCase().contains("gemini")) {
                                String shortName = mName.startsWith("models/") ? mName.substring(7) : mName;
                                validModels.add(shortName);
                            }
                        }

                        // Ưu tiên model chứa chữ "flash"
                        for (String vm : validModels) {
                            if (vm.toLowerCase().contains("flash")) {
                                bestModel = vm;
                                break;
                            }
                        }

                        if (bestModel == null && !validModels.isEmpty()) {
                            bestModel = validModels.get(0);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        if (bestModel == null || bestModel.isEmpty()) {
            bestModel = "gemini-2.5-flash";
        }
        return bestModel;
    }

    private void decomposeTasksWithGeminiAi() {
        if (taskList.isEmpty()) {
            Toast.makeText(this, "Không có công việc nào để phân tách!", Toast.LENGTH_SHORT).show();
            return;
        }

        String apiKey = getValidApiKey();

        btnAiDecomposeTasks.setEnabled(false);
        btnAiDecomposeTasks.setText("⚡ Gemini AI Đang Phân Tách Sâu...");

        new Thread(() -> {
            boolean isSuccess = false;
            String lastErrorMsg = "";

            try {
                // Tự động dò tìm tên model chính xác từ API Google
                String bestModel = discoverBestModelName(apiKey);

                JSONObject requestBody = new JSONObject();

                JSONObject sysInstruction = new JSONObject();
                JSONArray sysParts = new JSONArray();
                JSONObject sysPart = new JSONObject();
                sysPart.put("text", "Bạn là một Chuyên gia Quản lý Dự án & Trợ lý Học thuật Cá nhân cấp cao. Nhiệm vụ của bạn là phân tách các công việc thuộc " + phaseName + " thành danh sách kiểm tra (Checklist / Action Items) chi tiết, thực tế, bám sát 100% ngữ cảnh chủ đề của dự án.\n" +
                        "HÃY THỰC HIỆN TƯ DUY THEO CÁC BƯỚC SAU TRƯỚC KHI TRẢ VỀ KẾT QUẢ:\n" +
                        "- BƯỚC 1: Phân tích tiêu đề và mô tả của từng công việc để hiểu đúng 100% lĩnh vực chuyên môn.\n" +
                        "- BƯỚC 2: Chia nhỏ mỗi công việc thành 3-4 đầu việc cụ thể có thể hành động ngay, ước tính mỗi đầu việc nhỏ thực hiện trong 1 phiên Pomodoro (25 phút).\n" +
                        "- BƯỚC 3: Đảm bảo các đầu việc có tính nối tiếp, rõ ràng và có thể kiểm tra kết quả.\n" +
                        "RÀNG BUỘC ĐỊNH DẠNG TRẢ VỀ:\n" +
                        "Trả về DUY NHẤT một JSON Array chứa thông tin phân tách của tất cả các task. Mỗi JSON Object gồm:\n" +
                        "- \"taskIndex\": Chỉ số vị trí của task (0, 1, 2...)\n" +
                        "- \"subTasks\": Danh sách 3-4 chuỗi đầu việc cụ thể, thực tế bám sát tiêu đề và nội dung task.");
                sysParts.put(sysPart);
                sysInstruction.put("parts", sysParts);
                requestBody.put("system_instruction", sysInstruction);

                JSONArray contents = new JSONArray();
                JSONObject contentObj = new JSONObject();
                JSONArray contentParts = new JSONArray();

                StringBuilder sbTasks = new StringBuilder();
                for (int i = 0; i < taskList.size(); i++) {
                    sbTasks.append("Task ").append(i).append(": ").append(taskList.get(i).getTitle()).append("\nMô tả: ").append(taskList.get(i).getDescription()).append("\n\n");
                }

                JSONObject textPart = new JSONObject();
                textPart.put("text", sbTasks.toString());
                contentParts.put(textPart);
                contentObj.put("parts", contentParts);
                contents.put(contentObj);
                requestBody.put("contents", contents);

                // Cấu hình tham số suy luận: temperature = 0.3, responseMimeType = "application/json"
                JSONObject genConfig = new JSONObject();
                genConfig.put("temperature", 0.3);
                genConfig.put("responseMimeType", "application/json");
                requestBody.put("generationConfig", genConfig);

                // Thử model vừa dò tìm được, nếu lỗi thử các model dự phòng
                String[] modelsToTry = new String[]{bestModel, "gemini-2.5-flash", "gemini-2.0-flash", "gemini-flash-latest", "gemini-1.5-flash"};
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
                                            break;
                                        }
                                    }
                                }
                            }
                        } else {
                            lastErrorMsg = "HTTP " + responseCode + ": " + responseText;
                        }
                    } catch (Exception ex) {
                        lastErrorMsg = ex.getClass().getSimpleName() + " - " + ex.getMessage();
                    }
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
                        int tIdx = item.optInt("taskIndex", i);
                        JSONArray subArr = item.optJSONArray("subTasks");
                        if (subArr != null) {
                            List<String> stList = new ArrayList<>();
                            for (int j = 0; j < subArr.length(); j++) {
                                stList.add(subArr.getString(j));
                            }
                            taskSubTasksMap.put(tIdx, stList);
                        }
                    }
                    isSuccess = !taskSubTasksMap.isEmpty();
                }
            } catch (Exception e) {
                lastErrorMsg = e.getClass().getSimpleName() + " - " + e.getMessage();
            }

            final String errorDetails = lastErrorMsg;
            final boolean finalSuccess = isSuccess;

            new Handler(Looper.getMainLooper()).post(() -> {
                btnAiDecomposeTasks.setEnabled(true);
                btnAiDecomposeTasks.setText("⚡ AI Phân Tách Chi Tiết Đầu Việc Cần Làm");

                if (finalSuccess) {
                    renderTaskCards();
                    Toast.makeText(PhaseDetailActivity.this, "✨ Gemini AI đã phân tách các đầu việc chi tiết!", Toast.LENGTH_SHORT).show();
                } else {
                    // Tuyệt đối KHÔNG tự ý điền mock data/dữ liệu giả khi API lỗi. Hiển thị Dialog báo lỗi rõ ràng.
                    new AlertDialog.Builder(PhaseDetailActivity.this)
                            .setTitle("❌ Lỗi Gọi Gemini AI")
                            .setMessage("Không thể phân tách đầu việc bằng Gemini AI.\n\nChi tiết lỗi:\n" + (errorDetails.isEmpty() ? "Không nhận được phản hồi JSON hợp lệ" : errorDetails) + "\n\nVui lòng kiểm tra lại API Key hoặc kết nối mạng.")
                            .setPositiveButton("Đóng", null)
                            .setNeutralButton("Cấu hình API Key", (dialog, which) -> showApiKeyDialog(null))
                            .show();
                }
            });
        }).start();
    }
}
