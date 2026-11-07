package com.example.myapplication.adapter;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
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
            });
        }

        if (holder.btnEditTask != null) {
            holder.btnEditTask.setOnClickListener(v -> {
                android.widget.PopupMenu popup = new android.widget.PopupMenu(v.getContext(), v);
                popup.getMenu().add(0, 1, 0, "✏️ Chỉnh sửa công việc");
                popup.getMenu().add(0, 2, 1, "🗑️ Xóa công việc");
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1) {
                        Intent intent = new Intent(v.getContext(), AddTaskActivity.class);
                        intent.putExtra("TASK_ID", currentTask.getId());
                        intent.putExtra("PROJECT_ID", currentTask.getProjectId());
                        intent.putExtra("TASK_STATUS", currentTask.getStatus());
                        intent.putExtra("TASK_TITLE", currentTask.getTitle());
                        intent.putExtra("TASK_DESC", currentTask.getDescription());
                        intent.putExtra("TASK_LOC", currentTask.getLocation());
                        intent.putExtra("TASK_TIME", currentTask.getTime());
                        v.getContext().startActivity(intent);
                        return true;
                    } else if (item.getItemId() == 2) {
                        int currentPos = holder.getBindingAdapterPosition();
                        if (currentPos != RecyclerView.NO_POSITION) {
                            deleteTaskOnServer(holder.itemView.getContext(), currentTask, currentPos);
                        }
                        return true;
                    }
                    return false;
                });
                popup.show();
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
