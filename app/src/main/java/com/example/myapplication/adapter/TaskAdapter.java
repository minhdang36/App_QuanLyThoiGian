package com.example.myapplication.adapter;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Task;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.ui.task.AddTaskActivity;
import com.example.myapplication.ui.task.ProjectDetailActivity;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;

    public TaskAdapter(List<Task> taskList) {
        this.taskList = taskList;
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

        if (currentTask.getTitle() != null && !currentTask.getTitle().trim().isEmpty()) {
            holder.tvTaskTitle.setText(currentTask.getTitle());
        } else {
            holder.tvTaskTitle.setText("Chưa có tiêu đề");
        }

        if (currentTask.getDescription() != null && !currentTask.getDescription().trim().isEmpty()) {
            if (holder.tvTaskDesc != null) {
                holder.tvTaskDesc.setVisibility(View.VISIBLE);
                holder.tvTaskDesc.setText(currentTask.getDescription());
            }
        } else {
            if (holder.tvTaskDesc != null) {
                holder.tvTaskDesc.setVisibility(View.GONE);
            }
        }

        if (currentTask.getLocation() != null && !currentTask.getLocation().trim().isEmpty()) {
            if (holder.tvTaskLocation != null) {
                holder.tvTaskLocation.setText(currentTask.getLocation());
            }
        } else {
            if (holder.tvTaskLocation != null) {
                holder.tvTaskLocation.setText("Địa điểm linh hoạt");
            }
        }

        if (currentTask.getTime() != null && !currentTask.getTime().trim().isEmpty()) {
            holder.tvTaskTime.setText(currentTask.getTime());
        } else {
            holder.tvTaskTime.setText("08:00 AM - 10:00 AM");
        }

        if (holder.tvCategoryPill != null) {
            String categoryText = "🏷 Dự án " + currentTask.getProjectId();
            holder.tvCategoryPill.setText(categoryText);
        }

        // Đổi màu accent bên trái, viền card và icon theo Project ID
        int colorRes = getColorForProject(currentTask.getProjectId());
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
            holder.cbTaskStatus.setOnCheckedChangeListener(null);
            boolean isDone = "DONE".equalsIgnoreCase(currentTask.getStatus());
            holder.cbTaskStatus.setChecked(isDone);

            if (isDone) {
                holder.tvTaskTitle.setPaintFlags(holder.tvTaskTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                holder.tvTaskTitle.setTextColor(0xFF888888);
            } else {
                holder.tvTaskTitle.setPaintFlags(holder.tvTaskTitle.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                holder.tvTaskTitle.setTextColor(0xFF222222);
            }

            holder.cbTaskStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
                currentTask.setStatus(isChecked ? "DONE" : "PENDING");
                if (isChecked) {
                    holder.tvTaskTitle.setPaintFlags(holder.tvTaskTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                    holder.tvTaskTitle.setTextColor(0xFF888888);
                } else {
                    holder.tvTaskTitle.setPaintFlags(holder.tvTaskTitle.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                    holder.tvTaskTitle.setTextColor(0xFF222222);
                }
                updateTaskStatusOnServer(holder.itemView.getContext(), currentTask);
            });
        }

        if (holder.btnEditTask != null) {
            holder.btnEditTask.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), AddTaskActivity.class);
                intent.putExtra("TASK_ID", currentTask.getId());
                intent.putExtra("TASK_TITLE", currentTask.getTitle());
                intent.putExtra("TASK_DESC", currentTask.getDescription());
                intent.putExtra("TASK_LOC", currentTask.getLocation());
                intent.putExtra("TASK_TIME", currentTask.getTime());
                v.getContext().startActivity(intent);
            });
        }

        if (holder.btnDeleteTask != null) {
            holder.btnDeleteTask.setOnClickListener(v -> {
                deleteTaskOnServer(holder.itemView.getContext(), currentTask, position);
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

    private int getColorForProject(int projectId) {
        switch (projectId % 4) {
            case 1: return Color.parseColor("#10B981"); // Teal
            case 2: return Color.parseColor("#3B82F6"); // Blue
            case 3: return Color.parseColor("#F59E0B"); // Amber / Yellow
            case 0:
            default: return Color.parseColor("#8B5CF6"); // Purple
        }
    }

    private void updateTaskStatusOnServer(android.content.Context context, Task task) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiService apiService = retrofit.create(ApiService.class);
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
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiService apiService = retrofit.create(ApiService.class);
        apiService.deleteTask(task.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
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
        MaterialCardView cardTaskItem;
        View viewCategoryAccent;
        ImageView imgCategoryIcon;
        TextView tvTaskTime, tvTaskTitle, tvTaskDesc, tvTaskLocation, tvCategoryPill;
        CheckBox cbTaskStatus;
        ImageButton btnEditTask, btnDeleteTask;

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
        }
    }
}
