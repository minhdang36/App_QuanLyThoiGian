package com.example.myapplication.adapter;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FullCalendarAdapter extends RecyclerView.Adapter<FullCalendarAdapter.CalendarViewHolder> {

    public interface OnDayClickListener {
        void onDayClick(int dayNumber);
    }

    private List<String> daysOfMonth;
    private int selectedPosition = -1;
    private OnDayClickListener listener;
    private Set<Integer> daysWithEvents = new HashSet<>();

    public FullCalendarAdapter(List<String> daysOfMonth, int selectedPosition, OnDayClickListener listener) {
        this.daysOfMonth = daysOfMonth;
        this.selectedPosition = selectedPosition;
        this.listener = listener;
    }

    public FullCalendarAdapter(List<String> daysOfMonth) {
        this(daysOfMonth, -1, null);
    }

    public void setDaysWithEvents(Set<Integer> daysWithEvents) {
        this.daysWithEvents = daysWithEvents != null ? daysWithEvents : new HashSet<>();
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        int oldPos = selectedPosition;
        selectedPosition = position;
        if (oldPos >= 0 && oldPos < daysOfMonth.size()) notifyItemChanged(oldPos);
        if (selectedPosition >= 0 && selectedPosition < daysOfMonth.size()) notifyItemChanged(selectedPosition);
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_full_calendar_day, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        String dayText = daysOfMonth.get(position);
        holder.tvDateNumber.setText(dayText);

        if (position == selectedPosition && !dayText.isEmpty()) {
            holder.tvDateNumber.setBackgroundResource(R.drawable.bg_circle_brown);
            holder.tvDateNumber.setTextColor(Color.WHITE);
            holder.tvDateNumber.setTypeface(null, Typeface.BOLD);
        } else {
            holder.tvDateNumber.setBackgroundResource(android.R.color.transparent);
            holder.tvDateNumber.setTextColor(Color.parseColor("#333333"));
            holder.tvDateNumber.setTypeface(null, Typeface.NORMAL);
        }

        if (holder.viewEventDot != null) {
            if (!dayText.isEmpty()) {
                try {
                    int dayNum = Integer.parseInt(dayText);
                    if (daysWithEvents != null && daysWithEvents.contains(dayNum)) {
                        holder.viewEventDot.setVisibility(View.VISIBLE);
                    } else {
                        holder.viewEventDot.setVisibility(View.INVISIBLE);
                    }
                } catch (NumberFormatException e) {
                    holder.viewEventDot.setVisibility(View.INVISIBLE);
                }
            } else {
                holder.viewEventDot.setVisibility(View.INVISIBLE);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            int curPos = holder.getAdapterPosition();
            if (curPos == RecyclerView.NO_POSITION) return;

            String text = daysOfMonth.get(curPos);
            if (!text.isEmpty()) {
                int previousItem = selectedPosition;
                selectedPosition = curPos;
                notifyItemChanged(previousItem);
                notifyItemChanged(selectedPosition);

                if (listener != null) {
                    try {
                        int dayNum = Integer.parseInt(text);
                        listener.onDayClick(dayNum);
                    } catch (NumberFormatException ignored) {}
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return daysOfMonth != null ? daysOfMonth.size() : 0;
    }

    public static class CalendarViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateNumber;
        View viewEventDot;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateNumber = itemView.findViewById(R.id.tvDateNumber);
            viewEventDot = itemView.findViewById(R.id.viewEventDot);
        }
    }
}
