package com.example.myapplication.adapter;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.ProjectDetailActivity;
import com.example.myapplication.R;
import com.example.myapplication.model.Task;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;

    public TaskAdapter(List<Task> taskList) {
        this.taskList = taskList;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Nạp giao diện thẻ công việc item_task.xml
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task currentTask = taskList.get(position);

        // =========================================================
        // 1. GÁN DỮ LIỆU THẬT LÊN GIAO DIỆN ITEM (THẺ CÔNG VIỆC)
        // =========================================================

        // Gán Tiêu đề
        if (currentTask.getTitle() != null && !currentTask.getTitle().trim().isEmpty()) {
            holder.tvTaskTitle.setText(currentTask.getTitle());
        } else {
            holder.tvTaskTitle.setText("Chưa có tiêu đề");
        }

        // Gán Mô tả
        if (currentTask.getDescription() != null && !currentTask.getDescription().trim().isEmpty()) {
            holder.tvTaskDesc.setText(currentTask.getDescription());
        } else {
            holder.tvTaskDesc.setText("Không có mô tả");
        }

        // Gán Thời gian
        if (currentTask.getTime() != null && !currentTask.getTime().trim().isEmpty()) {
            holder.tvTaskTime.setText(currentTask.getTime());
        } else {
            holder.tvTaskTime.setText("--:--");
        }

        // Gán Địa điểm
        if (currentTask.getLocation() != null && !currentTask.getLocation().trim().isEmpty()) {
            holder.tvTaskLocation.setText(currentTask.getLocation());
        } else {
            holder.tvTaskLocation.setText("Không có địa điểm");
        }

        // =========================================================
        // 2. THÊM SỰ KIỆN CLICK VÀO ITEM ĐỂ MỞ TRANG CHI TIẾT
        // =========================================================
        holder.itemView.setOnClickListener(v -> {

            Intent intent = new Intent(v.getContext(), ProjectDetailActivity.class);

            // Gói dữ liệu thật của task này mang theo
            intent.putExtra("TASK_ID", currentTask.getId());
            intent.putExtra("TASK_TITLE", currentTask.getTitle());
            intent.putExtra("TASK_DESC", currentTask.getDescription());
            intent.putExtra("TASK_TIME", currentTask.getTime());
            intent.putExtra("TASK_LOC", currentTask.getLocation());
            intent.putExtra("TASK_STATUS", currentTask.getStatus());

            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskTime, tvTaskTitle, tvTaskDesc, tvTaskLocation;
        View viewPriorityColor;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskTime = itemView.findViewById(R.id.tvTaskTime);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvTaskDesc = itemView.findViewById(R.id.tvTaskDesc);
            tvTaskLocation = itemView.findViewById(R.id.tvTaskLocation);
            viewPriorityColor = itemView.findViewById(R.id.viewPriorityColor);
        }
    }
}