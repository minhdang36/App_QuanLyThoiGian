package com.example.myapplication.adapter;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Task;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.ui.task.AddTaskActivity;
import com.example.myapplication.ui.task.ProjectDetailActivity;
import com.google.android.material.card.MaterialCardView;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    // Màu chuẩn theo yêu cầu: Độ ưu tiên 1: Đỏ - Độ ưu tiên 2: Vàng - Độ ưu tiên 3: Xanh
    public static final int COLOR_PRIORITY_1 = 0xFFEF4444; // Đỏ (Ưu tiên 1 / Cao)
    public static final int COLOR_PRIORITY_2 = 0xFFF59E0B; // Vàng (Ưu tiên 2 / Trung bình)
    public static final int COLOR_PRIORITY_3 = 0xFF10B981; // Xanh lá (Ưu tiên 3 / Thấp)

    private List<Task> taskList;

    public interface OnTaskStatusChangeListener {
        void onStatusChanged(Task task, boolean isChecked);
    }

    private OnTaskStatusChangeListener onTaskStatusChangeListener;

    public void setOnTaskStatusChangeListener(OnTaskStatusChangeListener listener) {
        this.onTaskStatusChangeListener = listener;
    }

    public TaskAdapter(List<Task> taskList) {
        this.taskList = taskList;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        if (taskList != null && position >= 0 && position < taskList.size()) {
            Task t = taskList.get(position);
            if (t != null && t.getId() > 0) {
                return t.getId();
            }
        }
        return -position - 1L;
    }

    public void updateList(List<Task> newList) {
        this.taskList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task currentTask = taskList.get(position);

        String title = currentTask.getTitle();
        if (title != null && !title.trim().isEmpty()) {
            holder.tvTaskTitle.setText(title);
        } else {
            holder.tvTaskTitle.setText("Chưa có tiêu đề");
        }

        String desc = currentTask.getDescription();
        if (desc != null && !desc.trim().isEmpty()) {
            if (holder.tvTaskDesc != null) {
                holder.tvTaskDesc.setVisibility(View.VISIBLE);
                holder.tvTaskDesc.setText(desc);
            }
        } else {
            if (holder.tvTaskDesc != null) {
                holder.tvTaskDesc.setVisibility(View.GONE);
            }
        }

        String loc = currentTask.getLocation();
        if (loc != null && !loc.trim().isEmpty()) {
            if (holder.tvTaskLocation != null) {
                holder.tvTaskLocation.setText(loc);
            }
        } else {
            if (holder.tvTaskLocation != null) {
                holder.tvTaskLocation.setText("Địa điểm linh hoạt");
            }
        }

        String time = currentTask.getTime();
        if (time != null && !time.trim().isEmpty()) {
            holder.tvTaskTime.setText(formatDisplayTime(time));
        } else {
            holder.tvTaskTime.setText("08:00 AM - 10:00 AM");
        }

        if (holder.tvCategoryPill != null) {
            String cat = currentTask.getCategory();
            String categoryText = (cat != null && !cat.isEmpty())
                    ? "🏷 " + cat
                    : "🏷 Dự án " + currentTask.getProjectId();
            holder.tvCategoryPill.setText(categoryText);
        }

        // Đổi màu viền, dải accent và icon theo Mức độ ưu tiên chuẩn: Đỏ (Ưu 1) - Vàng (Ưu 2) - Xanh (Ưu 3)
        int colorRes = getColorForPriority(currentTask, position);
        if (holder.cardTaskItem != null) {
            holder.cardTaskItem.setStrokeColor(colorRes);
        }
        if (holder.viewCategoryAccent != null) {
            holder.viewCategoryAccent.setBackgroundColor(colorRes);
        }
        if (holder.imgCategoryIcon != null) {
            holder.imgCategoryIcon.setColorFilter(colorRes);
        }
        if (holder.cbTaskStatus != null) {
            holder.cbTaskStatus.setButtonTintList(ColorStateList.valueOf(colorRes));
        }

        final int primaryTextColor = holder.colorTextPrimary;
        final int secondaryTextColor = holder.colorTextSecondary;

        if (holder.cbTaskStatus != null) {
            holder.cbTaskStatus.setOnCheckedChangeListener(null);
            String s = currentTask.getStatus();
            boolean isDone = "DONE".equalsIgnoreCase(s) || "COMPLETED".equalsIgnoreCase(s) || "HOÀN THÀNH".equalsIgnoreCase(s);
            holder.cbTaskStatus.setChecked(isDone);

            if (isDone) {
                holder.tvTaskTitle.setPaintFlags(holder.tvTaskTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                holder.tvTaskTitle.setTextColor(secondaryTextColor);
            } else {
                holder.tvTaskTitle.setPaintFlags(holder.tvTaskTitle.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                holder.tvTaskTitle.setTextColor(primaryTextColor);
            }

            holder.cbTaskStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
                currentTask.setStatus(isChecked ? "DONE" : "PENDING");
                if (isChecked) {
                    holder.tvTaskTitle.setPaintFlags(holder.tvTaskTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                    holder.tvTaskTitle.setTextColor(secondaryTextColor);
                } else {
                    holder.tvTaskTitle.setPaintFlags(holder.tvTaskTitle.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                    holder.tvTaskTitle.setTextColor(primaryTextColor);
                }
                updateTaskStatusOnServer(holder.itemView.getContext(), currentTask);
                if (onTaskStatusChangeListener != null) {
                    onTaskStatusChangeListener.onStatusChanged(currentTask, isChecked);
                }
            });
        }

        if (holder.btnEditTask != null) {
            holder.btnEditTask.setOnClickListener(v -> {
                int currentPos = holder.getBindingAdapterPosition();
                showTaskOptionsDialog(v.getContext(), currentTask, currentPos);
            });
        }

        if (holder.btnDeleteTask != null) {
            holder.btnDeleteTask.setOnClickListener(v -> {
                int currentPos = holder.getBindingAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    deleteTaskOnServer(holder.itemView.getContext(), currentTask, currentPos);
                }
            });
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ProjectDetailActivity.class);
            intent.putExtra("TASK_ID", currentTask.getId());
            intent.putExtra("TASK_TITLE", currentTask.getTitle());
            intent.putExtra("TASK_DESC", currentTask.getDescription());
            intent.putExtra("TASK_TIME", currentTask.getTime());
            intent.putExtra("TASK_LOC", currentTask.getLocation());
            intent.putExtra("TASK_STATUS", currentTask.getStatus());
            intent.putExtra("PROJECT_ID", currentTask.getProjectId());
            intent.putExtra("TASK_PRIORITY", currentTask.getPriority());
            v.getContext().startActivity(intent);
        });
    }

    private String formatDisplayTime(String rawTime) {
        if (rawTime == null || !rawTime.contains("Hôm nay,")) return rawTime != null ? rawTime : "";
        Calendar today = Calendar.getInstance();
        int d = today.get(Calendar.DAY_OF_MONTH);
        int m = today.get(Calendar.MONTH) + 1;
        String todaySlash = d + "/" + m;
        String todayPad = String.format(Locale.US, "%02d/%02d", d, m);

        if (!rawTime.contains(todaySlash) && !rawTime.contains(todayPad)) {
            return rawTime.replace("Hôm nay,", "Ngày");
        }
        return rawTime;
    }

    public static int getColorForPriority(Task task, int position) {
        if (task == null) {
            return COLOR_PRIORITY_3; // Mặc định Xanh lá
        }

        String timeStr = task.getTime() != null ? task.getTime().toUpperCase(Locale.ROOT) : "";
        String priority = task.getPriority() != null ? task.getPriority().toUpperCase(Locale.ROOT) : "";
        String combined = priority + " " + timeStr;

        // 1. Mức Độ Ưu Tiên 1 ➔ MÀU ĐỎ (#EF4444)
        if (combined.contains("ƯU 1") || combined.contains("ƯU TIÊN 1") || combined.contains("MỨC 1") || combined.contains("CAO") || combined.contains("HIGH")) {
            return COLOR_PRIORITY_1;
        }

        // 2. Mức Độ Ưu Tiên 2 ➔ MÀU VÀNG (#F59E0B)
        if (combined.contains("ƯU 2") || combined.contains("ƯU TIÊN 2") || combined.contains("MỨC 2") || combined.contains("TRUNG BÌNH") || combined.contains("MEDIUM")) {
            return COLOR_PRIORITY_2;
        }

        // 3. Mức Độ Ưu Tiên 3 ➔ MÀU XANH LÁ (#10B981)
        if (combined.contains("ƯU 3") || combined.contains("ƯU TIÊN 3") || combined.contains("MỨC 3") || combined.contains("THẤP") || combined.contains("LOW")) {
            return COLOR_PRIORITY_3;
        }

        // Kiểm tra priorityLevel từ Model Task
        if (task.getPriorityLevel() == 1) return COLOR_PRIORITY_1;
        if (task.getPriorityLevel() == 2) return COLOR_PRIORITY_2;
        if (task.getPriorityLevel() == 3) return COLOR_PRIORITY_3;

        return COLOR_PRIORITY_3; // Mặc định Xanh lá
    }

    private void updateTaskStatusOnServer(android.content.Context context, Task task) {
        if (task.getId() <= 0) return;
        RetrofitClient.invalidateCache();
        ApiService apiService = RetrofitClient.getApiService();
        apiService.updateTask(task.getId(), task).enqueue(new Callback<Task>() {
            @Override
            public void onResponse(Call<Task> call, Response<Task> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(context, "Đã cập nhật trạng thái!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Task> call, Throwable t) {}
        });
    }

    private void deleteTaskOnServer(android.content.Context context, Task task, int position) {
        RetrofitClient.invalidateCache();
        ApiService apiService = RetrofitClient.getApiService();
        apiService.deleteTask(task.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful() && position >= 0 && position < taskList.size()) {
                    taskList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, taskList.size());
                    Toast.makeText(context, "Đã xóa công việc!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(context, "Lỗi kết nối xóa công việc!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showTaskOptionsDialog(android.content.Context context, Task currentTask, int currentPos) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);

        LinearLayout dialogCard = new LinearLayout(context);
        dialogCard.setOrientation(LinearLayout.VERTICAL);
        dialogCard.setBackgroundResource(R.drawable.bg_rounded_24dp);
        dialogCard.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.card_bg)));
        float density = context.getResources().getDisplayMetrics().density;
        int padding = Math.round(24 * density);
        int margin8 = Math.round(8 * density);
        int margin12 = Math.round(12 * density);
        int dp48 = Math.round(48 * density);
        int dp50 = Math.round(50 * density);
        int dp16 = Math.round(16 * density);

        dialogCard.setPadding(padding, padding, padding, padding);

        TextView tvTitle = new TextView(context);
        tvTitle.setText("⚙️ Tùy chọn công việc");
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        dialogCard.addView(tvTitle);

        TextView tvSub = new TextView(context);
        tvSub.setText(currentTask.getTitle() != null ? currentTask.getTitle() : "Công việc");
        tvSub.setTextSize(13);
        tvSub.setSingleLine(true);
        tvSub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tvSub.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        tvSub.setPadding(0, Math.round(4 * density), 0, margin12);
        dialogCard.addView(tvSub);

        builder.setView(dialogCard);
        androidx.appcompat.app.AlertDialog dialog = builder.create();

        LinearLayout btnEdit = new LinearLayout(context);
        btnEdit.setOrientation(LinearLayout.HORIZONTAL);
        btnEdit.setGravity(android.view.Gravity.CENTER_VERTICAL);
        btnEdit.setBackgroundResource(R.drawable.bg_search_bar);
        btnEdit.setPadding(dp16, 0, dp16, 0);
        LinearLayout.LayoutParams lpEdit = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp50);
        lpEdit.setMargins(0, 0, 0, margin8);
        btnEdit.setLayoutParams(lpEdit);

        TextView tvEditIcon = new TextView(context);
        tvEditIcon.setText("✏️ ");
        tvEditIcon.setTextSize(16);

        TextView tvEditText = new TextView(context);
        tvEditText.setText("Chỉnh sửa công việc");
        tvEditText.setTextSize(14);
        tvEditText.setTypeface(null, android.graphics.Typeface.BOLD);
        tvEditText.setTextColor(ContextCompat.getColor(context, R.color.text_primary));

        btnEdit.addView(tvEditIcon);
        btnEdit.addView(tvEditText);
        btnEdit.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(context, AddTaskActivity.class);
            intent.putExtra("TASK_ID", currentTask.getId());
            intent.putExtra("PROJECT_ID", currentTask.getProjectId());
            intent.putExtra("TASK_STATUS", currentTask.getStatus());
            intent.putExtra("TASK_TITLE", currentTask.getTitle());
            intent.putExtra("TASK_DESC", currentTask.getDescription());
            intent.putExtra("TASK_LOC", currentTask.getLocation());
            intent.putExtra("TASK_TIME", currentTask.getTime());
            context.startActivity(intent);
        });
        dialogCard.addView(btnEdit);

        LinearLayout btnDelete = new LinearLayout(context);
        btnDelete.setOrientation(LinearLayout.HORIZONTAL);
        btnDelete.setGravity(android.view.Gravity.CENTER_VERTICAL);
        btnDelete.setBackgroundResource(R.drawable.bg_search_bar);
        btnDelete.setPadding(dp16, 0, dp16, 0);
        LinearLayout.LayoutParams lpDelete = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp50);
        lpDelete.setMargins(0, 0, 0, margin12);
        btnDelete.setLayoutParams(lpDelete);

        TextView tvDelIcon = new TextView(context);
        tvDelIcon.setText("🗑️ ");
        tvDelIcon.setTextSize(16);

        TextView tvDelText = new TextView(context);
        tvDelText.setText("Xóa công việc");
        tvDelText.setTextSize(14);
        tvDelText.setTypeface(null, android.graphics.Typeface.BOLD);
        tvDelText.setTextColor(ContextCompat.getColor(context, R.color.brand_danger));

        btnDelete.addView(tvDelIcon);
        btnDelete.addView(tvDelText);
        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            if (currentPos != RecyclerView.NO_POSITION) {
                deleteTaskOnServer(context, currentTask, currentPos);
            }
        });
        dialogCard.addView(btnDelete);

        TextView btnCancel = new TextView(context);
        btnCancel.setText("HỦY");
        btnCancel.setTextSize(13);
        btnCancel.setTypeface(null, android.graphics.Typeface.BOLD);
        btnCancel.setGravity(android.view.Gravity.CENTER);
        btnCancel.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        btnCancel.setBackgroundResource(R.drawable.bg_pill_light);
        LinearLayout.LayoutParams lpCancel = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp48);
        btnCancel.setLayoutParams(lpCancel);
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialogCard.addView(btnCancel);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardTaskItem;
        final View viewCategoryAccent;
        final ImageView imgCategoryIcon;
        final TextView tvTaskTime, tvTaskTitle, tvTaskDesc, tvTaskLocation, tvCategoryPill;
        final CheckBox cbTaskStatus;
        final ImageButton btnEditTask, btnDeleteTask;
        final int colorTextPrimary, colorTextSecondary;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTaskItem = itemView.findViewById(R.id.cardTaskItem);
            viewCategoryAccent = itemView.findViewById(R.id.viewCategoryAccent);
            imgCategoryIcon = itemView.findViewById(R.id.imgCategoryIcon);
            tvTaskTime = itemView.findViewById(R.id.tvTaskTime);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvTaskDesc = itemView.findViewById(R.id.tvTaskDesc);
            tvTaskLocation = itemView.findViewById(R.id.tvTaskLocation);
            tvCategoryPill = itemView.findViewById(R.id.tvCategoryPill);
            cbTaskStatus = itemView.findViewById(R.id.cbTaskStatus);
            btnEditTask = itemView.findViewById(R.id.btnEditTask);
            btnDeleteTask = itemView.findViewById(R.id.btnDeleteTask);
            colorTextPrimary = ContextCompat.getColor(itemView.getContext(), R.color.text_primary);
            colorTextSecondary = ContextCompat.getColor(itemView.getContext(), R.color.text_secondary);
        }
    }
}
