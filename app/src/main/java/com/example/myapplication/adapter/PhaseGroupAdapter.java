package com.example.myapplication.adapter;

import android.content.Intent;
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
import com.example.myapplication.ui.phase.PhaseDetailActivity;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class PhaseGroupAdapter extends RecyclerView.Adapter<PhaseGroupAdapter.PhaseViewHolder> {

    private static final Gson GSON = new Gson();
    private final RecyclerView.RecycledViewPool sharedSubTaskPool = new RecyclerView.RecycledViewPool();
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
        return new PhaseViewHolder(view, sharedSubTaskPool);
    }

    @Override
    public void onBindViewHolder(@NonNull PhaseViewHolder holder, int position) {
        PhaseGroup phase = phaseList.get(position);

        holder.tvPhaseTitle.setText(phase.getPhaseName());
        int totalSub = phase.getSubTaskList().size();
        int doneSub = phase.getCompletedCount();
        holder.tvPhaseStats.setText(totalSub + " task nhỏ  •  " + doneSub + " đã xong");

        if (phase.getPhaseName().contains("Công việc hôm nay") || phase.getPhaseName().contains("Nhiệm vụ trong ngày")) {
            holder.tvExpandArrow.setVisibility(View.GONE);
            holder.layoutSubTasksContainer.setVisibility(View.VISIBLE);
            holder.subAdapter.updateList(phase.getSubTaskList());
        } else if (phase.isExpanded()) {
            holder.tvExpandArrow.setVisibility(View.VISIBLE);
            holder.tvExpandArrow.setText("▲ Thu gọn");
            holder.layoutSubTasksContainer.setVisibility(View.VISIBLE);
            holder.subAdapter.updateList(phase.getSubTaskList());
        } else {
            holder.tvExpandArrow.setVisibility(View.VISIBLE);
            holder.tvExpandArrow.setText("▼ Mở rộng");
            holder.layoutSubTasksContainer.setVisibility(View.GONE);
        }

        holder.tvExpandArrow.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            phase.setExpanded(!phase.isExpanded());
            notifyItemChanged(pos);
        });

        holder.layoutPhaseHeader.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), PhaseDetailActivity.class);
            intent.putExtra("PHASE_NAME", phase.getPhaseName());
            intent.putExtra("SUB_TASKS_JSON", GSON.toJson(phase.getSubTaskList()));
            intent.putExtra("IS_AI_PROJECT", true);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return phaseList != null ? phaseList.size() : 0;
    }

    public static class PhaseViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout layoutPhaseHeader, layoutSubTasksContainer;
        final TextView tvPhaseTitle, tvPhaseStats, tvExpandArrow;
        final RecyclerView rvPhaseSubTasks;
        final TaskAdapter subAdapter;

        public PhaseViewHolder(@NonNull View itemView, RecyclerView.RecycledViewPool pool) {
            super(itemView);
            layoutPhaseHeader = itemView.findViewById(R.id.layoutPhaseHeader);
            layoutSubTasksContainer = itemView.findViewById(R.id.layoutSubTasksContainer);
            tvPhaseTitle = itemView.findViewById(R.id.tvPhaseTitle);
            tvPhaseStats = itemView.findViewById(R.id.tvPhaseStats);
            tvExpandArrow = itemView.findViewById(R.id.tvExpandArrow);
            rvPhaseSubTasks = itemView.findViewById(R.id.rvPhaseSubTasks);

            LinearLayoutManager lm = new LinearLayoutManager(itemView.getContext());
            lm.setInitialPrefetchItemCount(4);
            rvPhaseSubTasks.setLayoutManager(lm);
            rvPhaseSubTasks.setNestedScrollingEnabled(false);
            rvPhaseSubTasks.setRecycledViewPool(pool);
            rvPhaseSubTasks.setItemAnimator(null);

            subAdapter = new TaskAdapter(new ArrayList<>());
            rvPhaseSubTasks.setAdapter(subAdapter);
        }
    }
}
