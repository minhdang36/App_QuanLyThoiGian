package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.WbsTask;

import java.util.List;

public class WbsTaskAdapter extends RecyclerView.Adapter<WbsTaskAdapter.WbsViewHolder> {

    private List<WbsTask> wbsTaskList;

    public WbsTaskAdapter(List<WbsTask> wbsTaskList) {
        this.wbsTaskList = wbsTaskList;
    }

    public void updateData(List<WbsTask> newList) {
        this.wbsTaskList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WbsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wbs_task, parent, false);
        return new WbsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WbsViewHolder holder, int position) {
        WbsTask task = wbsTaskList.get(position);

        holder.tvWbsPhase.setText(task.getPhaseName() != null ? task.getPhaseName() : "Giai đoạn");
        holder.tvWbsTitle.setText(task.getTaskTitle() != null ? task.getTaskTitle() : "Công việc");
        holder.tvWbsDesc.setText(task.getDescription() != null ? task.getDescription() : "Definition of Done: Hoàn thành đúng hạn");

        holder.tvWbsDate.setText("📅 " + (task.getScheduledDate() != null ? task.getScheduledDate() : "2026-10-18"));
        holder.tvWbsHours.setText("⏱ " + task.getEstimatedHours() + " giờ  •  🍅 " + task.getPomodoroCount() + " Pomodoro");

        String priority = task.getPriority() != null ? task.getPriority() : "Medium";
        holder.tvWbsPriority.setText(priority);

        holder.cbSelectTask.setOnCheckedChangeListener(null);
        holder.cbSelectTask.setChecked(task.isSelected());
        holder.cbSelectTask.setOnCheckedChangeListener((buttonView, isChecked) -> task.setSelected(isChecked));
    }

    @Override
    public int getItemCount() {
        return wbsTaskList != null ? wbsTaskList.size() : 0;
    }

    public static class WbsViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelectTask;
        TextView tvWbsPhase, tvWbsPriority, tvWbsTitle, tvWbsDesc, tvWbsDate, tvWbsHours;

        public WbsViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelectTask = itemView.findViewById(R.id.cbSelectTask);
            tvWbsPhase = itemView.findViewById(R.id.tvWbsPhase);
            tvWbsPriority = itemView.findViewById(R.id.tvWbsPriority);
            tvWbsTitle = itemView.findViewById(R.id.tvWbsTitle);
            tvWbsDesc = itemView.findViewById(R.id.tvWbsDesc);
            tvWbsDate = itemView.findViewById(R.id.tvWbsDate);
            tvWbsHours = itemView.findViewById(R.id.tvWbsHours);
        }
    }
}
