package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Task;

import java.util.Collections;
import java.util.List;

public class ScheduleTimelineAdapter extends RecyclerView.Adapter<ScheduleTimelineAdapter.TimelineViewHolder> {

    public interface OnTaskStatusChangeListener {
        void onStatusChanged();
    }

    private List<Task> taskList;
    private OnTaskStatusChangeListener statusChangeListener;

    public ScheduleTimelineAdapter(List<Task> taskList) {
        this.taskList = taskList;
    }

    public void setOnTaskStatusChangeListener(OnTaskStatusChangeListener listener) {
        this.statusChangeListener = listener;
    }

    public void updateData(List<Task> newList) {
        this.taskList = newList;
        notifyDataSetChanged();
    }

    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(taskList, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(taskList, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    @NonNull
    @Override
    public TimelineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_timeline_schedule, parent, false);
        return new TimelineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimelineViewHolder holder, int position) {
        Task task = taskList.get(position);

        // 1. Tiêu đề
        if (task.getTitle() != null && !task.getTitle().trim().isEmpty()) {
            holder.tvTimelineTitle.setText(task.getTitle());
        } else {
            holder.tvTimelineTitle.setText("Công việc");
        }

        // 2. Mô tả
        if (task.getDescription() != null && !task.getDescription().trim().isEmpty()) {
            holder.tvTimelineDesc.setText(task.getDescription());
        } else {
            holder.tvTimelineDesc.setText("Không có ghi chú thêm");
        }

        // 3. Thời gian hiển thị bên trái & Mức độ ưu tiên
        String timeStr = task.getTime() != null ? task.getTime() : "";
        String priorityTag = "";
        String combined = (task.getPriority() != null ? task.getPriority() : "") + " " + timeStr;
        String pUpper = combined.toUpperCase();

        if (!timeStr.contains("🚩")) {
            if (pUpper.contains("ƯU 1") || pUpper.contains("CAO") || pUpper.contains("HIGH")) {
                priorityTag = "\n🚩 Ưu 1";
            } else if (pUpper.contains("ƯU 2") || pUpper.contains("TRUNG BÌNH") || pUpper.contains("MEDIUM")) {
                priorityTag = "\n🚩 Ưu 2";
            } else if (pUpper.contains("ƯU 3") || pUpper.contains("THẤP") || pUpper.contains("LOW")) {
                priorityTag = "\n🚩 Ưu 3";
            }
        }

        if (timeStr.contains("🕒")) {
            String[] parts = timeStr.split("🕒");
            if (parts.length >= 2) {
                String clockStr = parts[1].trim().replace(" - ", "\n");
                holder.tvTimelineTime.setText(clockStr + priorityTag);
            } else {
                holder.tvTimelineTime.setText(timeStr + priorityTag);
            }
        } else if (!timeStr.trim().isEmpty()) {
            holder.tvTimelineTime.setText(timeStr.replace(" | ", "\n") + priorityTag);
        } else {
            holder.tvTimelineTime.setText("08:00\n10:00" + priorityTag);
        }

        // 4. Phân loại Tag: Lịch học cố định vs AI xếp lịch
        if (task.getProjectId() == 2 || (task.getLocation() != null && task.getLocation().contains("PMT"))) {
            holder.tvTimelineTagAi.setText("📌 Lịch cố định");
            holder.tvTimelineTagAi.setBackgroundResource(R.drawable.bg_pill_light);
            holder.tvTimelineTagAi.setTextColor(0xFF555555);
            holder.tvTimelineTagCategory.setText("🏷 Học tập");
        } else {
            holder.tvTimelineTagAi.setText("⚡ FlowAI Xếp");
            holder.tvTimelineTagAi.setBackgroundResource(R.drawable.bg_pill_brown);
            holder.tvTimelineTagAi.setTextColor(0xFFFFFFFF);
            holder.tvTimelineTagCategory.setText("🏷 Công việc");
        }

        // 5. Địa điểm & Trạng thái
        if (task.getLocation() != null && !task.getLocation().trim().isEmpty()) {
            holder.tvTimelineSubInfo.setText("📍 " + task.getLocation());
        } else {
            holder.tvTimelineSubInfo.setText("📍 Hoàn thành công việc");
        }

        boolean isDone = "DONE".equalsIgnoreCase(task.getStatus()) || "COMPLETED".equalsIgnoreCase(task.getStatus());
        holder.cbTimelineStatus.setOnCheckedChangeListener(null);
        holder.cbTimelineStatus.setChecked(isDone);

        holder.cbTimelineStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setStatus(isChecked ? "DONE" : "PENDING");
            if (statusChangeListener != null) {
                statusChangeListener.onStatusChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    public static class TimelineViewHolder extends RecyclerView.ViewHolder {
        TextView tvTimelineTime, tvTimelineTagAi, tvTimelineTagCategory, tvTimelineDuration;
        TextView tvTimelineTitle, tvTimelineDesc, tvTimelineSubInfo;
        CheckBox cbTimelineStatus;

        public TimelineViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimelineTime = itemView.findViewById(R.id.tvTimelineTime);
            tvTimelineTagAi = itemView.findViewById(R.id.tvTimelineTagAi);
            tvTimelineTagCategory = itemView.findViewById(R.id.tvTimelineTagCategory);
            tvTimelineDuration = itemView.findViewById(R.id.tvTimelineDuration);
            tvTimelineTitle = itemView.findViewById(R.id.tvTimelineTitle);
            tvTimelineDesc = itemView.findViewById(R.id.tvTimelineDesc);
            tvTimelineSubInfo = itemView.findViewById(R.id.tvTimelineSubInfo);
            cbTimelineStatus = itemView.findViewById(R.id.cbTimelineStatus);
        }
    }
}
