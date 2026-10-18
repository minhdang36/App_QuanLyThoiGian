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
import com.example.myapplication.ui.project.AddProjectActivity;
import com.example.myapplication.ui.task.ProjectTasksActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

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

        // ==========================================
        // 1. NHẤN BÌNH THƯỜNG: Mở danh sách công việc
        // ==========================================
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ProjectTasksActivity.class);
            // Truyền ID và Tên của Dự án sang màn hình danh sách
            intent.putExtra("PROJECT_ID", project.getId());
            intent.putExtra("PROJECT_NAME", project.getName());
            v.getContext().startActivity(intent);
        });

        // ==========================================
        // 2. NHẤN GIỮ (LONG CLICK): Mở Menu Sửa / Xóa
        // ==========================================
        holder.itemView.setOnLongClickListener(v -> {
            CharSequence[] options = new CharSequence[]{"Sửa danh mục", "Xóa danh mục"};
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(v.getContext());
            builder.setTitle("Tùy chọn: " + project.getName());

            builder.setItems(options, (dialog, which) -> {
                if (which == 0) {
                    // CHỌN SỬA -> Mở AddProjectActivity ở chế độ Sửa
                    Intent intent = new Intent(v.getContext(), AddProjectActivity.class);
                    intent.putExtra("PROJECT_ID", project.getId());
                    intent.putExtra("PROJECT_NAME", project.getName());
                    intent.putExtra("PROJECT_DESC", project.getDescription());
                    v.getContext().startActivity(intent);

                } else if (which == 1) {
                    // CHỌN XÓA -> Hiển thị hộp thoại xác nhận
                    new android.app.AlertDialog.Builder(v.getContext())
                            .setTitle("Xác nhận xóa")
                            .setMessage("Bạn có chắc muốn xóa dự án này không?")
                            .setPositiveButton("Xóa", (dialogDelete, whichDelete) -> {
                                // Gọi hàm xóa đã được thêm ở cuối file
                                deleteProjectFromApi(project.getId(), holder.getAdapterPosition(), v.getContext());
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                }
            });
            builder.show();
            return true; // Trả về true để hệ thống hiểu là đã xử lý xong sự kiện nhấn giữ
        });

        // ==========================================
        // 3. HIỂN THỊ DỮ LIỆU LÊN GIAO DIỆN
        // ==========================================
        holder.tvProjectName.setText(project.getName() + " ●");

        // Tránh lỗi null khi MySQL chưa có mô tả
        if (project.getDescription() != null && !project.getDescription().isEmpty()) {
            holder.tvProjectDesc.setText(project.getDescription());
        } else {
            holder.tvProjectDesc.setText("Chưa có mô tả");
        }

        holder.tvTaskCount.setText(project.getTotalTasks() + " Công việc");

        // Kiểm tra an toàn cho Icon (nếu có id)
        if (project.getIconResId() != 0) {
            holder.imgProjectIcon.setImageResource(project.getIconResId());
        }

        // Xử lý tiến độ (Progress)
        int progress = project.getProgressPercentage();
        holder.tvProgressPercent.setText(progress + "%");
        holder.progressBar.setProgress(progress);
        holder.tvFinished.setText(project.getFinishedTasks() + " đã xong");
        holder.tvRemaining.setText(project.getRemainingTasks() + " còn lại");

        // Xử lý màu sắc an toàn
        String colorStr = project.getColorHex();
        int projectColor;

        if (colorStr != null && !colorStr.trim().isEmpty()) {
            try {
                projectColor = Color.parseColor(colorStr);
            } catch (IllegalArgumentException e) {
                projectColor = Color.parseColor("#4DB6AC"); // Màu Teal mặc định nếu sai format
            }
        } else {
            projectColor = Color.parseColor("#4DB6AC");
        }

        holder.imgProjectIcon.setBackgroundTintList(ColorStateList.valueOf(projectColor));
        holder.progressBar.setProgressTintList(ColorStateList.valueOf(projectColor));

        if (holder.btnAiBreakdownItem != null) {
            holder.btnAiBreakdownItem.setOnClickListener(v -> {
                Intent aiIntent = new Intent(v.getContext(), com.example.myapplication.ui.ai.AiTaskBreakdownActivity.class);
                aiIntent.putExtra("MAIN_TASK_TITLE", project.getName());
                v.getContext().startActivity(aiIntent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return projectList != null ? projectList.size() : 0;
    }

    // ==========================================
    // HÀM GỌI API XÓA PROJECT (Nằm bên trong Adapter)
    // ==========================================
    private void deleteProjectFromApi(int projectId, int position, android.content.Context context) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        apiService.deleteProject(projectId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Xóa thành công, cập nhật lại giao diện danh sách ngay lập tức
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

    // ==========================================
    // CLASS VIEWHOLDER
    // ==========================================
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