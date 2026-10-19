package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.PhaseGroup;

import java.util.List;

public class PhaseGroupAdapter extends RecyclerView.Adapter<PhaseGroupAdapter.PhaseViewHolder> {

    private List<PhaseGroup> phaseList;

    public PhaseGroupAdapter(List<PhaseGroup> phaseList) {
        this.phaseList = phaseList;
    }

    public void updateData(List<PhaseGroup> newList) {
        this.phaseList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PhaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_phase_group, parent, false);
        return new PhaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhaseViewHolder holder, int position) {
        PhaseGroup phase = phaseList.get(position);

        holder.tvPhaseTitle.setText(phase.getPhaseName());
        int totalSub = phase.getSubTaskList().size();
        int doneSub = phase.getCompletedCount();
        holder.tvPhaseStats.setText(totalSub + " task nhỏ  •  " + doneSub + " đã xong");

        if (phase.isExpanded()) {
            holder.tvExpandArrow.setText("▲ Thu gọn");
            holder.layoutSubTasksContainer.setVisibility(View.VISIBLE);
        } else {
            holder.tvExpandArrow.setText("▼ Mở rộng");
            holder.layoutSubTasksContainer.setVisibility(View.GONE);
        }

        // Đổ danh sách task nhỏ vào RecyclerView bên trong Phase Card
        holder.rvPhaseSubTasks.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        TaskAdapter subAdapter = new TaskAdapter(phase.getSubTaskList());
        holder.rvPhaseSubTasks.setAdapter(subAdapter);

        holder.layoutPhaseHeader.setOnClickListener(v -> {
            phase.setExpanded(!phase.isExpanded());
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return phaseList != null ? phaseList.size() : 0;
    }

    public static class PhaseViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutPhaseHeader, layoutSubTasksContainer;
        TextView tvPhaseTitle, tvPhaseStats, tvExpandArrow;
        RecyclerView rvPhaseSubTasks;

        public PhaseViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutPhaseHeader = itemView.findViewById(R.id.layoutPhaseHeader);
            layoutSubTasksContainer = itemView.findViewById(R.id.layoutSubTasksContainer);
            tvPhaseTitle = itemView.findViewById(R.id.tvPhaseTitle);
            tvPhaseStats = itemView.findViewById(R.id.tvPhaseStats);
            tvExpandArrow = itemView.findViewById(R.id.tvExpandArrow);
            rvPhaseSubTasks = itemView.findViewById(R.id.rvPhaseSubTasks);
        }
    }
}
