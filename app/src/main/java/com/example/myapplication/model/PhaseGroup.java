package com.example.myapplication.model;

import java.util.ArrayList;
import java.util.List;

public class PhaseGroup {
    private String phaseName;
    private List<Task> subTaskList = new ArrayList<>();
    private boolean isExpanded = false;

    public PhaseGroup(String phaseName) {
        this.phaseName = phaseName;
    }

    public String getPhaseName() { return phaseName; }
    public void setPhaseName(String phaseName) { this.phaseName = phaseName; }

    public List<Task> getSubTaskList() { return subTaskList; }

    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }

    public int getCompletedCount() {
        int count = 0;
        for (Task t : subTaskList) {
            String s = t.getStatus() != null ? t.getStatus().toUpperCase() : "";
            if ("DONE".equals(s) || "COMPLETED".equals(s)) {
                count++;
            }
        }
        return count;
    }
}
