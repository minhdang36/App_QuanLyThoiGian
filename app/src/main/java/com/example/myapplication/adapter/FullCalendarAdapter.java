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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FullCalendarAdapter extends RecyclerView.Adapter<FullCalendarAdapter.CalendarViewHolder> {

    private static final int MAX_DOTS = 5;
    private static final int DEFAULT_DOT_COLOR = Color.parseColor("#38BDF8");

    public interface OnDayClickListener {
        void onDayClick(int dayNumber);
    }

    private List<String> daysOfMonth;
    private int selectedPosition = -1;
    private final OnDayClickListener listener;
    private Set<Integer> daysWithEvents = new HashSet<>();
    private Map<Integer, List<Integer>> dayTaskColors = new HashMap<>();

    public FullCalendarAdapter(List<String> daysOfMonth, int selectedPosition, OnDayClickListener listener) {
        this.daysOfMonth = daysOfMonth;
        this.selectedPosition = selectedPosition;
        this.listener = listener;
        setHasStableIds(true);
    }

    public FullCalendarAdapter(List<String> daysOfMonth) {
        this(daysOfMonth, -1, null);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void updateCalendarData(
            List<String> newDaysOfMonth,
            int newSelectedPosition,
            Set<Integer> events,
            Map<Integer, List<Integer>> colors
    ) {
        this.daysOfMonth = newDaysOfMonth;
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
        if (oldPos >= 0 && daysOfMonth != null && oldPos < daysOfMonth.size()) {
            notifyItemChanged(oldPos);
        }
        if (selectedPosition >= 0 && daysOfMonth != null && selectedPosition < daysOfMonth.size()) {
            notifyItemChanged(selectedPosition);
        }
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
            holder.tvDateNumber.setBackgroundResource(R.drawable.bg_selected_day_squircle);
            holder.tvDateNumber.setTextColor(Color.WHITE);
            holder.tvDateNumber.setTypeface(null, Typeface.BOLD);
        } else {
            holder.tvDateNumber.setBackgroundResource(android.R.color.transparent);
            holder.tvDateNumber.setTextColor(holder.colorTextPrimary);
            holder.tvDateNumber.setTypeface(null, Typeface.NORMAL);
        }

        if (holder.dotContainer != null) {
            if (!dayText.isEmpty()) {
                int dayNum = parsePositiveInt(dayText);
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
            } else {
                holder.dotContainer.setVisibility(View.INVISIBLE);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            int curPos = holder.getAdapterPosition();
            if (curPos == RecyclerView.NO_POSITION) return;

            String text = daysOfMonth.get(curPos);
            if (!text.isEmpty()) {
                setSelectedPosition(curPos);
                if (listener != null) {
                    int dayNum = parsePositiveInt(text);
                    if (dayNum > 0) {
                        listener.onDayClick(dayNum);
                    }
                }
            }
        });
    }

    private static int parsePositiveInt(String s) {
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
        return daysOfMonth != null ? daysOfMonth.size() : 0;
    }

    public static class CalendarViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDateNumber;
        final View viewEventDot;
        final ViewGroup dotContainer;
        final View[] dotViews = new View[MAX_DOTS];
        final int colorTextPrimary;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateNumber = itemView.findViewById(R.id.tvDateNumber);
            viewEventDot = itemView.findViewById(R.id.viewEventDot);
            colorTextPrimary = ContextCompat.getColor(itemView.getContext(), R.color.text_primary);

            if (viewEventDot instanceof ViewGroup) {
                dotContainer = (ViewGroup) viewEventDot;
                dotContainer.removeAllViews();
                float density = itemView.getResources().getDisplayMetrics().density;
                int dotSize = Math.round(4f * density);
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
