package com.example.myapplication.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.CalendarDate;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {

    private static final int MAX_DOTS = 5;
    private static final int DEFAULT_DOT_COLOR = Color.parseColor("#38BDF8");

    public interface OnDateClickListener {
        void onDateClick(int dayNumber);
    }

    private List<CalendarDate> dateList;
    private int selectedPosition = 0;
    private final OnDateClickListener listener;
    private Set<Integer> daysWithEvents = new HashSet<>();
    private Map<Integer, List<Integer>> dayTaskColors = new HashMap<>();

    public CalendarAdapter(List<CalendarDate> dateList, int selectedPosition, OnDateClickListener listener) {
        this.dateList = dateList;
        this.selectedPosition = selectedPosition;
        this.listener = listener;
        setHasStableIds(true);
    }

    public CalendarAdapter(List<CalendarDate> dateList) {
        this(dateList, 0, null);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void updateCalendarData(
            List<CalendarDate> newDateList,
            int newSelectedPosition,
            Set<Integer> events,
            Map<Integer, List<Integer>> colors
    ) {
        this.dateList = newDateList;
        this.selectedPosition = newSelectedPosition;
        this.daysWithEvents = events != null ? events : new HashSet<>();
        this.dayTaskColors = colors != null ? colors : new HashMap<>();
        notifyDataSetChanged();
    }

    public void setDaysWithEvents(Set<Integer> daysWithEvents) {
        this.daysWithEvents = daysWithEvents != null ? daysWithEvents : new HashSet<>();
        notifyDataSetChanged();
    }

    public void setDayTaskColors(Map<Integer, List<Integer>> dayTaskColors) {
        this.dayTaskColors = dayTaskColors != null ? dayTaskColors : new HashMap<>();
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        if (this.selectedPosition == position) return;
        int oldPos = selectedPosition;
        selectedPosition = position;
        if (oldPos >= 0 && dateList != null && oldPos < dateList.size()) {
            notifyItemChanged(oldPos);
        }
        if (selectedPosition >= 0 && dateList != null && selectedPosition < dateList.size()) {
            notifyItemChanged(selectedPosition);
        }
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
            if (holder.layoutDay != null) {
                holder.layoutDay.setBackgroundResource(R.drawable.bg_day_capsule_selected);
            }
            holder.tvDateNumber.setBackgroundResource(android.R.color.transparent);
            holder.tvDayOfWeek.setTextColor(holder.colorBrandPrimary);
            holder.tvDateNumber.setTextColor(holder.colorTextPrimary);
            holder.tvDateNumber.setTypeface(null, Typeface.BOLD);
            holder.tvDayOfWeek.setTypeface(null, Typeface.BOLD);
        } else {
            if (holder.layoutDay != null) {
                holder.layoutDay.setBackgroundResource(R.drawable.bg_day_capsule_unselected);
            }
            holder.tvDateNumber.setBackgroundResource(android.R.color.transparent);
            holder.tvDayOfWeek.setTextColor(holder.colorTextSecondary);
            holder.tvDateNumber.setTextColor(holder.colorTextPrimary);
            holder.tvDateNumber.setTypeface(null, Typeface.BOLD);
            holder.tvDayOfWeek.setTypeface(null, Typeface.NORMAL);
        }

        if (holder.dotContainer != null) {
            int dayNum = parsePositiveInt(date.getDateNumber());
            List<Integer> colors = dayNum > 0 ? dayTaskColors.get(dayNum) : null;
            if ((colors == null || colors.isEmpty()) && dayNum > 0 && daysWithEvents.contains(dayNum)) {
                colors = Collections.singletonList(DEFAULT_DOT_COLOR);
            }

            if (colors != null && !colors.isEmpty()) {
                holder.dotContainer.setVisibility(View.VISIBLE);
                int maxDots = Math.min(colors.size(), MAX_DOTS);
                for (int i = 0; i < MAX_DOTS; i++) {
                    View dot = holder.dotViews[i];
                    if (dot == null) continue;
                    if (i < maxDots) {
                        dot.setVisibility(View.VISIBLE);
                        dot.setBackgroundTintList(ColorStateList.valueOf(colors.get(i)));
                    } else {
                        dot.setVisibility(View.GONE);
                    }
                }
            } else {
                holder.dotContainer.setVisibility(View.INVISIBLE);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) return;

            setSelectedPosition(currentPosition);

            if (listener != null) {
                int dayNum = parsePositiveInt(dateList.get(currentPosition).getDateNumber());
                if (dayNum > 0) {
                    listener.onDateClick(dayNum);
                }
            }
        });
    }

    private static int parsePositiveInt(String s) {
        if (s == null || s.isEmpty()) return -1;
        int res = 0;
        for (int i = 0, len = s.length(); i < len; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return -1;
            res = res * 10 + (c - '0');
        }
        return res;
    }

    @Override
    public int getItemCount() {
        return dateList != null ? dateList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDayOfWeek, tvDateNumber;
        final LinearLayout layoutDay;
        final View viewEventDot;
        final ViewGroup dotContainer;
        final View[] dotViews = new View[MAX_DOTS];
        final int colorTextPrimary, colorTextSecondary, colorBrandPrimary;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayOfWeek = itemView.findViewById(R.id.tvDayOfWeek);
            tvDateNumber = itemView.findViewById(R.id.tvDateNumber);
            layoutDay = itemView.findViewById(R.id.layoutDay);
            viewEventDot = itemView.findViewById(R.id.viewEventDot);
            colorTextPrimary = ContextCompat.getColor(itemView.getContext(), R.color.text_primary);
            colorTextSecondary = ContextCompat.getColor(itemView.getContext(), R.color.text_secondary);
            colorBrandPrimary = ContextCompat.getColor(itemView.getContext(), R.color.brand_primary);

            if (viewEventDot instanceof ViewGroup) {
                dotContainer = (ViewGroup) viewEventDot;
                dotContainer.removeAllViews();
                float density = itemView.getResources().getDisplayMetrics().density;
                int dotSize = Math.round(4.5f * density);
                int dotMargin = Math.round(1.5f * density);

                for (int i = 0; i < MAX_DOTS; i++) {
                    View dot = new View(itemView.getContext());
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dotSize, dotSize);
                    if (i > 0) {
                        params.setMarginStart(dotMargin * 2);
                    }
                    dot.setLayoutParams(params);
                    dot.setBackgroundResource(R.drawable.bg_circle_teal);
                    dot.setVisibility(View.GONE);
                    dotContainer.addView(dot);
                    dotViews[i] = dot;
                }
            } else {
                dotContainer = null;
            }
        }
    }
}
