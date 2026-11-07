package com.example.myapplication.ui.phase;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
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
import androidx.core.content.ContextCompat;

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

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

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

        for (int i = 0; i < taskList.size(); i++) {
            taskSubTasksMap.put(i, generateContextualSubTasks(taskList.get(i)));
        }

        renderTaskCards();

        if (btnAiDecomposeTasks != null) {
            btnAiDecomposeTasks.setOnClickListener(v -> decomposeTasksWithGeminiAi());
        }
    }

    private void renderTaskCards() {
        if (layoutTasksContainer == null) return;
        layoutTasksContainer.removeAllViews();

        int cardBgColor = ContextCompat.getColor(this, R.color.card_bg);
        int textPrimaryColor = ContextCompat.getColor(this, R.color.text_primary);
        int textSecondaryColor = ContextCompat.getColor(this, R.color.text_secondary);

        for (int i = 0; i < taskList.size(); i++) {
            Task task = taskList.get(i);

            MaterialCardView cardView = new MaterialCardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, dpToPx(16));
            cardView.setLayoutParams(cardParams);
            cardView.setCardBackgroundColor(cardBgColor);
            cardView.setRadius(dpToPx(20));
            cardView.setCardElevation(0);

            LinearLayout cardContent = new LinearLayout(this);
            cardContent.setOrientation(LinearLayout.VERTICAL);
            int pad = dpToPx(18);
            cardContent.setPadding(pad, pad, pad, pad);

            TextView tvTitle = new TextView(this);
            tvTitle.setText("📌 " + (task.getTitle() != null ? task.getTitle() : "Công việc"));
            tvTitle.setTextColor(textPrimaryColor);
            tvTitle.setTextSize(16);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            cardContent.addView(tvTitle);

            if (task.getTime() != null && !task.getTime().isEmpty()) {
                TextView tvTime = new TextView(this);
                tvTime.setText(task.getTime());
                tvTime.setTextColor(0xFF2563EB);
                tvTime.setTextSize(12);
                tvTime.setPadding(0, dpToPx(4), 0, 0);
                cardContent.addView(tvTime);
            }

            if (task.getDescription() != null && !task.getDescription().isEmpty()) {
                TextView tvDesc = new TextView(this);
                tvDesc.setText(task.getDescription());
                tvDesc.setTextColor(textSecondaryColor);
                tvDesc.setTextSize(13);
                tvDesc.setPadding(0, dpToPx(6), 0, 0);
                cardContent.addView(tvDesc);
            }

            LinearLayout layoutChecklist = new LinearLayout(this);
            layoutChecklist.setOrientation(LinearLayout.VERTICAL);
            layoutChecklist.setTag("checklist_" + i);
            layoutChecklist.setPadding(0, dpToPx(8), 0, 0);

            List<String> subTasks = taskSubTasksMap.get(i);
            if (subTasks != null && !subTasks.isEmpty()) {
                TextView tvHeaderChecklist = new TextView(this);
                tvHeaderChecklist.setText("📋 Các đầu việc cần làm (Tích chọn để thêm vào task):");
                tvHeaderChecklist.setTextColor(textPrimaryColor);
                tvHeaderChecklist.setTextSize(12);
                tvHeaderChecklist.setTypeface(null, android.graphics.Typeface.BOLD);
                tvHeaderChecklist.setPadding(0, dpToPx(4), 0, dpToPx(6));
                layoutChecklist.addView(tvHeaderChecklist);

                List<CheckBox> checkBoxesList = new ArrayList<>();
                for (String st : subTasks) {
                    CheckBox checkBox = new CheckBox(this);
                    checkBox.setText(st);
                    checkBox.setChecked(true);
                    checkBox.setTextColor(textPrimaryColor);
                    checkBox.setTextSize(13);
                    checkBox.setButtonTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
                    checkBoxesList.add(checkBox);
                    layoutChecklist.addView(checkBox);
                }

                Button btnAddSelected = new Button(this);
                btnAddSelected.setText("➕ Thêm các đầu việc đã chọn vào Nhiệm vụ phụ");
                btnAddSelected.setTextColor(0xFFFFFFFF);
                btnAddSelected.setTextSize(12);
                btnAddSelected.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
                LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                btnLp.setMargins(0, dpToPx(10), 0, 0);
                btnAddSelected.setLayoutParams(btnLp);

                btnAddSelected.setOnClickListener(v -> {
                    List<String> selectedSubtasks = new ArrayList<>();
                    for (CheckBox cb : checkBoxesList) {
                        if (cb.isChecked()) {
                            selectedSubtasks.add(cb.getText().toString());
                        }
                    }

                    if (selectedSubtasks.isEmpty()) {
                        Toast.makeText(this, "Vui lòng tích chọn ít nhất 1 đầu việc!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveSelectedSubTasksToTask(task.getTitle(), selectedSubtasks);
                    Toast.makeText(this, "✔ Đã thêm " + selectedSubtasks.size() + " đầu việc vào Nhiệm vụ phụ của task!", Toast.LENGTH_SHORT).show();
                });

                layoutChecklist.addView(btnAddSelected);
            } else {
                TextView tvHint = new TextView(this);
                tvHint.setText("💡 Bấm nút 'AI Phân Tách' ở trên để Gemini AI đào sâu phân tách danh sách đầu việc chi tiết cho công việc này.");
                tvHint.setTextColor(textSecondaryColor);
                tvHint.setTextSize(11);
                tvHint.setPadding(0, dpToPx(4), 0, 0);
                layoutChecklist.addView(tvHint);
            }

            cardContent.addView(layoutChecklist);
            cardView.addView(cardContent);
            layoutTasksContainer.addView(cardView);
        }
    }

    private void saveSelectedSubTasksToTask(String taskTitle, List<String> newSubTasks) {
        if (taskTitle == null || taskTitle.trim().isEmpty() || newSubTasks == null || newSubTasks.isEmpty()) return;
        SharedPreferences pref = getSharedPreferences("TaskSubTasks", MODE_PRIVATE);
        Gson gson = new Gson();

        String cleanTitle = taskTitle;
        if (cleanTitle.startsWith("[") && cleanTitle.contains("]")) {
            cleanTitle = cleanTitle.substring(cleanTitle.indexOf("]") + 1).trim();
        }

        String json = pref.getString("SUBTASKS_" + cleanTitle.hashCode(), null);
        List<String> currentSubtasks = new ArrayList<>();
        if (json != null && !json.trim().isEmpty()) {
            try {
                Type listType = new TypeToken<ArrayList<String>>() {}.getType();
                List<String> parsed = gson.fromJson(json, listType);
                if (parsed != null) currentSubtasks.addAll(parsed);
            } catch (Exception ignored) {}
        }

        for (String st : newSubTasks) {
            if (!currentSubtasks.contains(st)) {
                currentSubtasks.add(st);
            }
        }

        pref.edit().putString("SUBTASKS_" + cleanTitle.hashCode(), gson.toJson(currentSubtasks)).apply();
        pref.edit().putString("SUBTASKS_" + taskTitle.hashCode(), gson.toJson(currentSubtasks)).apply();
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
        layout.setPadding(dpToPx(20), dpToPx(12), dpToPx(20), dpToPx(4));

        TextView tvGuide = new TextView(this);
        tvGuide.setText("Dán API Key Gemini của bạn (AIzaSy...) để kích hoạt AI cao cấp nhất từ Google.\n(Lấy Key miễn phí 100% tại aistudio.google.com)");
        tvGuide.setTextSize(13);
        tvGuide.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        layout.addView(tvGuide);

        final EditText edtKey = new EditText(this);
        edtKey.setHint("Dán API Key (AIzaSy...) tại đây");
        if (!activeKey.contains("DÁN_API_KEY")) {
            edtKey.setText(activeKey);
        }
        edtKey.setTextSize(13);
        edtKey.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
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

    private void decomposeTasksWithGeminiAi() {
        if (taskList.isEmpty()) {
            Toast.makeText(this, "Không có công việc nào để phân tách!", Toast.LENGTH_SHORT).show();
            return;
        }

        String apiKey = getValidApiKey();

        if (apiKey.isEmpty() || apiKey.contains("DÁN_API_KEY")) {
            showApiKeyDialog(this::decomposeTasksWithGeminiAi);
            return;
        }

        btnAiDecomposeTasks.setEnabled(false);
        btnAiDecomposeTasks.setText("⚡ Gemini AI Đang Phân Tách Sâu...");

        new Thread(() -> {
            try {
                JSONObject requestBody = new JSONObject();

                JSONObject sysInstruction = new JSONObject();
                JSONArray sysParts = new JSONArray();
                JSONObject sysPart = new JSONObject();
                sysPart.put("text", "Bạn là một Chuyên gia Quản lý Dự án & Trợ lý Học thuật Cá nhân cấp cao. Nhiệm vụ của bạn là phân tách các công việc thuộc " + phaseName + " thành danh sách kiểm tra (Checklist / Action Items) chi tiết, thực tế, BÁM SÁT 100% THEO TỪNG TIÊU ĐỀ TASK KHÁC NHAU. MỖI TASK PHẢI CÓ CÁC ĐẦU VIỆC KHÁC NHAU, KHÔNG ĐƯỢC LẶP LẠI GIỐNG NHAU.\n" +
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
                    sbTasks.append("Task ").append(i).append(" (TIÊU ĐỀ: ").append(taskList.get(i).getTitle()).append("):\nMô tả: ").append(taskList.get(i).getDescription()).append("\n\n");
                }

                JSONObject textPart = new JSONObject();
                textPart.put("text", sbTasks.toString());
                contentParts.put(textPart);
                contentObj.put("parts", contentParts);
                contents.put(contentObj);
                requestBody.put("contents", contents);

                JSONObject genConfig = new JSONObject();
                genConfig.put("temperature", 0.4);
                genConfig.put("responseMimeType", "application/json");
                requestBody.put("generationConfig", genConfig);

                String[] targetUrls = new String[]{
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey,
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey,
                        "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent?key=" + apiKey,
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent?key=" + apiKey
                };

                String rawJsonArrayStr = "";

                for (String targetUrl : targetUrls) {
                    try {
                        URL url = new URL(targetUrl);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                        conn.setDoOutput(true);
                        conn.setConnectTimeout(25000);
                        conn.setReadTimeout(25000);

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
                }
            } catch (Exception ignored) {}

            for (int i = 0; i < taskList.size(); i++) {
                if (!taskSubTasksMap.containsKey(i) || taskSubTasksMap.get(i) == null || taskSubTasksMap.get(i).isEmpty()) {
                    taskSubTasksMap.put(i, generateContextualSubTasks(taskList.get(i)));
                }
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                btnAiDecomposeTasks.setEnabled(true);
                btnAiDecomposeTasks.setText("⚡ AI Phân Tách Chi Tiết Đầu Việc Cần Làm");
                renderTaskCards();
                Toast.makeText(PhaseDetailActivity.this, "✨ AI đã phân tách các đầu việc riêng biệt cho từng task! Tích chọn và bấm 'Thêm vào Nhiệm vụ phụ'.", Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    private List<String> generateContextualSubTasks(Task task) {
        List<String> list = new ArrayList<>();
        String rawTitle = task.getTitle() != null ? task.getTitle() : "Công việc";
        String cleanTitle = rawTitle;

        if (rawTitle.startsWith("[") && rawTitle.contains("]")) {
            cleanTitle = rawTitle.substring(rawTitle.indexOf("]") + 1).trim();
        }

        String fullText = (rawTitle + " " + (task.getDescription() != null ? task.getDescription() : "")).toLowerCase();

        // 1. Tiếng Trung / HSK / Hán ngữ
        if (fullText.contains("tiếng trung") || fullText.contains("hsk") || fullText.contains("hán ngữ")) {
            if (fullText.contains("từ vựng") || fullText.contains("ghi nhớ") || fullText.contains("500 từ") || fullText.contains("từ mới")) {
                list.add("Sưu tầm và phân loại 50 từ vựng HSK6 theo chủ đề cho: " + cleanTitle);
                list.add("Tra cứu Pinyin, âm Hán Việt, cách viết nét bút và bộ thủ của các từ mới.");
                list.add("Tạo bộ thẻ ghi nhớ Spaced Repetition (Anki/Quizlet) để tự ôn tập 20 phút mỗi ngày.");
                list.add("Đặt câu ví dụ thực tế cho 10 từ vựng khó nhất để ghi nhớ ngữ cảnh sâu.");
            } else if (fullText.contains("ngữ pháp") || fullText.contains("cấu trúc") || fullText.contains("phân tích")) {
                list.add("Phân tích 10 điểm ngữ pháp HSK6 cao cấp xuất hiện trong bài thi cho: " + cleanTitle);
                list.add("Soạn 5 câu ví dụ áp dụng thực tế cho từng cấu trúc ngữ pháp vừa học.");
                list.add("Làm bài tập ứng dụng ngữ pháp HSK6 và tự chấm điểm phân tích lỗi sai.");
            } else if (fullText.contains("đề thi") || fullText.contains("thi thử") || fullText.contains("test") || fullText.contains("luyện")) {
                list.add("Giải 1 đề thi thử HSK6 phần Nghe / Đọc nghiêm túc trong thời gian thực.");
                list.add("Chấm điểm và ghi chép chi tiết danh sách các câu làm sai vào sổ tay.");
                list.add("Dịch lại các đoạn văn đọc hiểu HSK6 dài để bổ sung vốn từ học thuật.");
            } else {
                list.add("Chuẩn bị giáo trình và tài liệu luyện tập chuyên sâu cho: " + cleanTitle);
                list.add("Luyện nghe tin tức Tiếng Trung (CCTV/RFI) 15 phút mỗi ngày để tăng phản xạ.");
                list.add("Luyện viết đoạn văn 80 chữ HSK6 áp dụng các từ vựng và ngữ pháp đã học.");
            }
        }
        // 2. TOPIK / Tiếng Hàn
        else if (fullText.contains("topik") || fullText.contains("tiếng hàn")) {
            if (fullText.contains("từ vựng") || fullText.contains("thành ngữ")) {
                list.add("Sưu tầm 100 từ vựng học thuật Hán Hàn (한자어) thường xuất hiện trong TOPIK II.");
                list.add("Lập danh sách 30 tục ngữ và thành ngữ 4 chữ (사자성어) kèm ví dụ ngữ cảnh.");
                list.add("Sử dụng phương pháp Flashcard Anki để ôn tập từ vựng TOPIK II hằng ngày.");
            } else if (fullText.contains("cấu trúc") || fullText.contains("đề thi") || fullText.contains("test")) {
                list.add("Giải đề thi thử TOPIK II nghiêm túc trong thời gian thực để đánh giá tốc độ.");
                list.add("Chấm điểm và phân loại chi tiết các lỗi sai vào bảng thống kê.");
                list.add("Phân tích cấu trúc 4 câu hỏi phần Viết (51-54) TOPIK II.");
            } else {
                list.add("Luyện nghe các đoạn tin tức KBS/YTN News 15 phút và tóm tắt nội dung.");
                list.add("Viết 1 đoạn văn 200 chữ áp dụng các biểu hiện ngữ pháp cao cấp TOPIK II.");
                list.add("Rà soát bảng từ vựng đã học và kiểm tra lại phản xạ dịch câu.");
            }
        }
        // 3. TOEIC / Tiếng Anh
        else if (fullText.contains("toeic") || fullText.contains("tiếng anh") || fullText.contains("english")) {
            if (fullText.contains("từ vựng") || fullText.contains("part")) {
                list.add("Học 30 từ vựng TOEIC chuyên ngành kinh tế/thương mại theo từng Part.");
                list.add("Luyện nghe Part 1 & Part 2 TOEIC với tốc độ 1.2x để nâng cao phản xạ.");
                list.add("Ghi chép lại các bẫy phát âm và từ đồng nghĩa thường gặp trong đề thi.");
            } else {
                list.add("Giải 1 đề ETS TOEIC Listening & Reading đầy đủ 200 câu trong 120 phút.");
                list.add("Tra cứu đáp án chi tiết và dịch lại các câu sai trong Part 5 & Part 7.");
                list.add("Tổng hợp từ vựng mới rút ra từ đề thi vào ứng dụng Quizlet.");
            }
        }
        // 4. Các công việc Lập trình / Công nghệ / Dự án
        else if (fullText.contains("phát triển") || fullText.contains("lập trình") || fullText.contains("code") || fullText.contains("chức năng") || fullText.contains("module")) {
            list.add("Khởi tạo cấu trúc module và khai báo interface cho: " + cleanTitle);
            list.add("Lập trình xử lý logic cốt lõi và kết nối dữ liệu API/Database.");
            list.add("Sửa các lỗi phát sinh (debug) và kiểm thử tính mượt mà của tính năng.");
            list.add("Rà soát mã nguồn (code review) và viết tài liệu hướng dẫn kỹ thuật.");
        }
        else if (fullText.contains("thiết kế") || fullText.contains("giao diện") || fullText.contains("ui") || fullText.contains("ux")) {
            list.add("Phác thảo sơ đồ Wireframe cho: " + cleanTitle);
            list.add("Lựa chọn bảng màu UI, phông chữ và bộ icon chuẩn thương hiệu.");
            list.add("Xây dựng bản Prototype tương tác mượt mà và kiểm tra trải nghiệm người dùng.");
        }
        else if (fullText.contains("mục tiêu") || fullText.contains("phạm vi") || fullText.contains("kế hoạch")) {
            list.add("Phân tích 5 yêu cầu cốt lõi và phạm vi công việc của: " + cleanTitle);
            list.add("Soạn tài liệu mô tả mục tiêu ban đầu và bảng tiêu chuẩn hoàn thành (DoD).");
            list.add("Lập kế hoạch phân bổ thời gian và mốc thời gian thực hiện chi tiết.");
        }
        // 5. Mặc định bám sát 100% tiêu đề công việc
        else {
            list.add("Chuẩn bị tài liệu và công cụ thực thi chi tiết cho: " + cleanTitle);
            list.add("Thực hiện các bước xử lý cốt lõi theo đúng yêu cầu công việc.");
            list.add("Kiểm tra, rà soát chất lượng sản phẩm và nghiệm thu cho: " + cleanTitle);
        }

        return list;
    }
}
