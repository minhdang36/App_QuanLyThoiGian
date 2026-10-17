package com.example.myapplication.adapter;

import android.graphics.Color;
import android.graphics.Typeface;
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

    public interface OnDateClickListener {
        void onDateClick(int dayNumber);
    }

    private List<CalendarDate> dateList;
    private int selectedPosition = 0;
    private OnDateClickListener listener;

    public CalendarAdapter(List<CalendarDate> dateList, int selectedPosition, OnDateClickListener listener) {
        this.dateList = dateList;
        this.selectedPosition = selectedPosition;
        this.listener = listener;
    }

    public CalendarAdapter(List<CalendarDate> dateList) {
        this(dateList, 0, null);
    }

    public void setSelectedPosition(int position) {
        int oldPos = selectedPosition;
        selectedPosition = position;
        if (oldPos >= 0 && oldPos < dateList.size()) notifyItemChanged(oldPos);
        if (selectedPosition >= 0 && selectedPosition < dateList.size()) notifyItemChanged(selectedPosition);
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

        if (selectedPosition == position) {
            holder.tvDateNumber.setTextColor(Color.parseColor("#362C2A"));
            holder.tvDayOfWeek.setTextColor(Color.parseColor("#362C2A"));
            holder.tvDateNumber.setTypeface(null, Typeface.BOLD);
            holder.tvDayOfWeek.setTypeface(null, Typeface.BOLD);
        } else {
            holder.tvDateNumber.setTextColor(Color.parseColor("#888888"));
            holder.tvDayOfWeek.setTextColor(Color.parseColor("#888888"));
            holder.tvDateNumber.setTypeface(null, Typeface.NORMAL);
            holder.tvDayOfWeek.setTypeface(null, Typeface.NORMAL);
        }

        holder.itemView.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) return;

            int previousPosition = selectedPosition;
            selectedPosition = currentPosition;
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                try {
                    int dayNum = Integer.parseInt(dateList.get(currentPosition).getDateNumber());
                    listener.onDateClick(dayNum);
                } catch (NumberFormatException ignored) {}
            }
        });
    }

    @Override
    public int getItemCount() {
        return dateList != null ? dateList.size() : 0;
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
