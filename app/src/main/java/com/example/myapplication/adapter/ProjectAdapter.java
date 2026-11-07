package com.example.myapplication.adapter;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Project;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.ui.project.AddProjectActivity;
import com.example.myapplication.ui.task.ProjectTasksActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private List<Project> projectList;

    public ProjectAdapter(List<Project> projectList) {
        this.projectList = projectList;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projectList.get(position);

        // 1. NHẤN BÌNH THƯỜNG: Mở danh sách công việc
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ProjectTasksActivity.class);
            intent.putExtra("PROJECT_ID", project.getId());
            intent.putExtra("PROJECT_NAME", project.getName());
            v.getContext().startActivity(intent);
        });

        // 2. NHẤN GIỮ (LONG CLICK): Mở Menu Sửa / Xóa
        holder.itemView.setOnLongClickListener(v -> {
            CharSequence[] options = new CharSequence[]{"Sửa danh mục", "Xóa danh mục"};
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(v.getContext());
            builder.setTitle("Tùy chọn: " + project.getName());

            builder.setItems(options, (dialog, which) -> {
                if (which == 0) {
                    Intent intent = new Intent(v.getContext(), AddProjectActivity.class);
                    intent.putExtra("PROJECT_ID", project.getId());
                    intent.putExtra("PROJECT_NAME", project.getName());
                    intent.putExtra("PROJECT_DESC", project.getDescription());
                    v.getContext().startActivity(intent);

                } else if (which == 1) {
                    new android.app.AlertDialog.Builder(v.getContext())
                            .setTitle("Xác nhận xóa")
                            .setMessage("Bạn có chắc muốn xóa dự án này không?")
                            .setPositiveButton("Xóa", (dialogDelete, whichDelete) -> {
                                int curPos = holder.getBindingAdapterPosition();
                                if (curPos != RecyclerView.NO_POSITION) {
                                    deleteProjectFromApi(project.getId(), curPos, v.getContext());
                                }
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                }
            });
            builder.show();
            return true;
        });

        // 3. HIỂN THỊ DỮ LIỆU LÊN GIAO DIỆN
        holder.tvProjectName.setText(project.getName() + " ●");

        if (project.getDescription() != null && !project.getDescription().isEmpty()) {
            holder.tvProjectDesc.setText(project.getDescription());
        } else {
            holder.tvProjectDesc.setText("Chưa có mô tả");
        }

        holder.tvTaskCount.setText(project.getTotalTasks() + " Công việc");

        if (project.getIconResId() != 0) {
            holder.imgProjectIcon.setImageResource(project.getIconResId());
        }

        int progress = project.getProgressPercentage();
        holder.tvProgressPercent.setText(progress + "%");
        holder.progressBar.setProgress(progress);
        holder.tvFinished.setText(project.getFinishedTasks() + " đã xong");
        holder.tvRemaining.setText(project.getRemainingTasks() + " còn lại");

        String colorStr = project.getColorHex();
        int[] modernPalette = new int[]{
                Color.parseColor("#2563EB"), // Royal Blue
                Color.parseColor("#38BDF8"), // Sky Blue
                Color.parseColor("#4F46E5"), // Indigo
                Color.parseColor("#0EA5E9"), // Ocean Blue
                Color.parseColor("#64748B")  // Cool Slate
        };
        int fallbackColor = modernPalette[Math.abs(position) % modernPalette.length];
        int projectColor = fallbackColor;

        if (colorStr != null && !colorStr.trim().isEmpty()) {
            String normalized = colorStr.trim().toUpperCase();
            if (normalized.equals("#362C2A") || normalized.equals("#004D40") ||
                normalized.equals("#5D4037") || normalized.equals("#8D6E63")) {
                projectColor = fallbackColor;
            } else {
                try {
                    projectColor = Color.parseColor(normalized);
                } catch (IllegalArgumentException e) {
                    projectColor = fallbackColor;
                }
            }
        }

        holder.imgProjectIcon.setBackgroundTintList(ColorStateList.valueOf(projectColor));
        holder.progressBar.setProgressTintList(ColorStateList.valueOf(projectColor));

        String pName = project.getName() != null ? project.getName().trim() : "";
        boolean isDefaultCategory = pName.contains("Công việc") ||
                                   pName.contains("Học tập") || pName.contains("Học Tập") ||
                                   pName.contains("Cá nhân") || pName.contains("Cá Nhân") ||
                                   pName.contains("Sức khỏe") || pName.contains("Thể thao") ||
                                   (project.getId() >= 1 && project.getId() <= 4);

        if (holder.btnAiBreakdownItem != null) {
            if (isDefaultCategory) {
                holder.btnAiBreakdownItem.setVisibility(View.GONE);
            } else {
                holder.btnAiBreakdownItem.setVisibility(View.VISIBLE);
                holder.btnAiBreakdownItem.setOnClickListener(v -> {
                    Intent aiIntent = new Intent(v.getContext(), com.example.myapplication.ui.ai.AiTaskBreakdownActivity.class);
                    aiIntent.putExtra("MAIN_TASK_TITLE", project.getName());
                    aiIntent.putExtra("MAIN_TASK_GOAL", project.getDescription());
                    aiIntent.putExtra("CREATED_PROJECT_ID", project.getId());
                    v.getContext().startActivity(aiIntent);
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return projectList != null ? projectList.size() : 0;
    }

    private void deleteProjectFromApi(int projectId, int position, android.content.Context context) {
        ApiService apiService = RetrofitClient.getApiService();
        apiService.deleteProject(projectId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful() && position >= 0 && position < projectList.size()) {
                    projectList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, projectList.size());
                    android.widget.Toast.makeText(context, "Đã xóa thành công!", android.widget.Toast.LENGTH_SHORT).show();
                } else {
                    android.widget.Toast.makeText(context, "Xóa thất bại (Lỗi: " + response.code() + ")", android.widget.Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                android.widget.Toast.makeText(context, "Lỗi kết nối mạng", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static class ProjectViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProjectIcon;
        TextView tvProjectName, tvProjectDesc, tvTaskCount, tvProgressPercent, tvFinished, tvRemaining, btnAiBreakdownItem;
        ProgressBar progressBar;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProjectIcon = itemView.findViewById(R.id.imgProjectIcon);
            tvProjectName = itemView.findViewById(R.id.tvProjectName);
            tvProjectDesc = itemView.findViewById(R.id.tvProjectDesc);
            tvTaskCount = itemView.findViewById(R.id.tvTaskCount);
            tvProgressPercent = itemView.findViewById(R.id.tvProgressPercent);
            progressBar = itemView.findViewById(R.id.progressBar);
            tvFinished = itemView.findViewById(R.id.tvFinished);
            tvRemaining = itemView.findViewById(R.id.tvRemaining);
            btnAiBreakdownItem = itemView.findViewById(R.id.btnAiBreakdownItem);
        }
    }
}
