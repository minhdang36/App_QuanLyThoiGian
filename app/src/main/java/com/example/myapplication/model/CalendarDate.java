package com.example.myapplication.model;

public class CalendarDate {
    private String dayOfWeek;  // Ví dụ: Mon, Tue
    private String dateNumber; // Ví dụ: 02, 03
    private boolean isSelected;

    public CalendarDate(String dayOfWeek, String dateNumber, boolean isSelected) {
        this.dayOfWeek = dayOfWeek;
        this.dateNumber = dateNumber;
        this.isSelected = isSelected;
    }

    public String getDayOfWeek() { return dayOfWeek; }
    public String getDateNumber() { return dateNumber; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}
