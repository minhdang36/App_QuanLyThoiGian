package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class Task {

    @SerializedName("id")
    private int id;

    @SerializedName("projectId")
    private int projectId;

    @SerializedName("status")
    private String status;

    @SerializedName("time")
    private String time;

    @SerializedName("priority")
    private int priorityLevel = 3;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    private transient String priority;
    private String location;
    private int userId;
    private String category;
    private String completedAtTime;
    private int totalPomodoros = 4;
    private int completedPomodoros = 0;
    private int totalFocusTimeMinutes = 0;
    private Integer parentTaskId;

    public Task() {}

    public Task(int id, String title, String description, String time, String location, String status, int projectId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.time = time;
        this.location = location;
        this.status = status;
        this.projectId = projectId;
    }

    public int getId() { return id; }
    public int getProjectId() { return projectId; }
    public String getStatus() { return status; }
    public String getTime() { return time; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public int getUserId() { return userId; }
    public String getCategory() { return category; }
    public String getCompletedAtTime() { return completedAtTime; }
    public int getPriorityLevel() { return priorityLevel; }
    public int getTotalPomodoros() { return totalPomodoros; }
    public int getCompletedPomodoros() { return completedPomodoros; }
    public int getTotalFocusTimeMinutes() { return totalFocusTimeMinutes; }
    public Integer getParentTaskId() { return parentTaskId; }

    public String getPriority() {
        if (priority != null && !priority.trim().isEmpty()) {
            return priority;
        }
        if (priorityLevel == 1) return "🚩 Ưu 1";
        if (priorityLevel == 2) return "🚩 Ưu 2";
        if (priorityLevel == 3) return "🚩 Ưu 3";
        return "🚩 Ưu 3";
    }

    public void setId(int id) { this.id = id; }
    public void setProjectId(int projectId) { this.projectId = projectId; }
    public void setStatus(String status) { this.status = status; }

    public void setTime(String time) {
        this.time = time;
        if (time != null) {
            String upper = time.toUpperCase();
            if (upper.contains("ƯU 1") || upper.contains("HIGH") || upper.contains("CAO")) {
                this.priorityLevel = 1;
            } else if (upper.contains("ƯU 2") || upper.contains("MEDIUM") || upper.contains("TRUNG BÌNH")) {
                this.priorityLevel = 2;
            } else if (upper.contains("ƯU 3") || upper.contains("LOW") || upper.contains("THẤP")) {
                this.priorityLevel = 3;
            }
        }
    }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setLocation(String location) { this.location = location; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setCategory(String category) { this.category = category; }
    public void setCompletedAtTime(String completedAtTime) { this.completedAtTime = completedAtTime; }
    public void setPriorityLevel(int priorityLevel) { this.priorityLevel = priorityLevel; }
    public void setTotalPomodoros(int totalPomodoros) { this.totalPomodoros = totalPomodoros; }
    public void setCompletedPomodoros(int completedPomodoros) { this.completedPomodoros = completedPomodoros; }
    public void setTotalFocusTimeMinutes(int totalFocusTimeMinutes) { this.totalFocusTimeMinutes = totalFocusTimeMinutes; }
    public void setParentTaskId(Integer parentTaskId) { this.parentTaskId = parentTaskId; }

    public void setPriority(String priority) {
        this.priority = priority;
        if (priority != null) {
            String upper = priority.toUpperCase();
            if (upper.contains("ƯU 1") || upper.contains("HIGH") || upper.contains("CAO")) {
                this.priorityLevel = 1;
            } else if (upper.contains("ƯU 2") || upper.contains("MEDIUM") || upper.contains("TRUNG BÌNH")) {
                this.priorityLevel = 2;
            } else if (upper.contains("ƯU 3") || upper.contains("LOW") || upper.contains("THẤP")) {
                this.priorityLevel = 3;
            }
        }
    }
}
