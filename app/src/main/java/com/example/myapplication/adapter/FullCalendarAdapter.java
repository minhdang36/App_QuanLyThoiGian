package com.example.myapplication.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

import java.util.List;

public class FullCalendarAdapter extends RecyclerView.Adapter<FullCalendarAdapter.CalendarViewHolder> {

    private List<String> daysOfMonth;
    private int selectedPosition = 1; // Mặc định chọn ngày mùng 2 (vị trí index 1)

    public FullCalendarAdapter(List<String> daysOfMonth) {
        this.daysOfMonth = daysOfMonth;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng lại layout 1 ô ngày bạn đã tạo ở bước trước
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_full_calendar_day, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        String dayText = daysOfMonth.get(position);
        holder.tvDateNumber.setText(dayText);

        // Đổi màu nền nếu là ngày được chọn
        if (position == selectedPosition && !dayText.isEmpty()) {
            holder.tvDateNumber.setBackgroundResource(R.drawable.bg_circle_brown);
            holder.tvDateNumber.setTextColor(Color.WHITE);
        } else {
            holder.tvDateNumber.setBackgroundResource(android.R.color.transparent);
            holder.tvDateNumber.setTextColor(Color.parseColor("#333333"));
        }

        // Bắt sự kiện click để đổi ngày
        holder.itemView.setOnClickListener(v -> {
            if (!dayText.isEmpty()) {
                int previousItem = selectedPosition;
                selectedPosition = position;
                notifyItemChanged(previousItem);
                notifyItemChanged(selectedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return daysOfMonth.size();
    }

    public static class CalendarViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateNumber;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ ID từ file item_full_calendar_day.xml
            tvDateNumber = itemView.findViewById(R.id.tvDateNumber);
        }
    }
}