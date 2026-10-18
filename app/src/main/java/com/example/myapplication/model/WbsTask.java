package com.example.myapplication.model;

public class WbsTask {
    private String phaseName;
    private String taskTitle;
    private String description;
    private String scheduledDate;
    private double estimatedHours;
    private String priority;
    private int pomodoroCount;
    private boolean selected = true;

    public WbsTask() {
    }

    public WbsTask(String phaseName, String taskTitle, String description, String scheduledDate, double estimatedHours, String priority, int pomodoroCount) {
        this.phaseName = phaseName;
        this.taskTitle = taskTitle;
        this.description = description;
        this.scheduledDate = scheduledDate;
        this.estimatedHours = estimatedHours;
        this.priority = priority;
        this.pomodoroCount = pomodoroCount;
    }

    public String getPhaseName() { return phaseName; }
    public void setPhaseName(String phaseName) { this.phaseName = phaseName; }

    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(String scheduledDate) { this.scheduledDate = scheduledDate; }

    public double getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(double estimatedHours) { this.estimatedHours = estimatedHours; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public int getPomodoroCount() { return pomodoroCount; }
    public void setPomodoroCount(int pomodoroCount) { this.pomodoroCount = pomodoroCount; }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
}
