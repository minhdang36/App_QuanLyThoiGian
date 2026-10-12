package com.example.myapplication.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.CalendarDate;

import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {

    private List<CalendarDate> dateList;
    private int selectedPosition = 0; // Mặc định chọn ngày đầu tiên

    public CalendarAdapter(List<CalendarDate> dateList) {
        this.dateList = dateList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CalendarDate date = dateList.get(position);
        holder.tvDayOfWeek.setText(date.getDayOfWeek());
        holder.tvDateNumber.setText(date.getDateNumber());

        // Đổi màu sắc nếu ngày đó được chọn
        if (selectedPosition == position) {
            holder.tvDateNumber.setTextColor(Color.parseColor("#000000")); // Đen đậm
            holder.tvDayOfWeek.setTextColor(Color.parseColor("#000000"));
        } else {
            holder.tvDateNumber.setTextColor(Color.parseColor("#888888")); // Xám
            holder.tvDayOfWeek.setTextColor(Color.parseColor("#888888"));
        }

        // Bắt sự kiện click chọn ngày
        holder.itemView.setOnClickListener(v -> {
            // Khắc phục lỗi: Lấy vị trí thực tế tại thời điểm click
            int currentPosition = holder.getAdapterPosition();

            // Tránh lỗi crash nếu item bị click trong lúc đang bị xóa khỏi danh sách
            if (currentPosition == RecyclerView.NO_POSITION) return;

            int previousPosition = selectedPosition;
            selectedPosition = currentPosition;
            notifyItemChanged(previousPosition); // Cập nhật lại giao diện ô cũ
            notifyItemChanged(selectedPosition); // Cập nhật giao diện ô mới được chọn
        });
    }

    @Override
    public int getItemCount() {
        return dateList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayOfWeek, tvDateNumber;
        LinearLayout layoutDay;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayOfWeek = itemView.findViewById(R.id.tvDayOfWeek);
            tvDateNumber = itemView.findViewById(R.id.tvDateNumber);
            layoutDay = itemView.findViewById(R.id.layoutDay);
        }
    }
}