package com.example.myapplication.model;

public class Task {
    private int id;
    private int projectId;
    private String status;
    private String time;
    private String title;
    private String description;
    private String location;
    private int userId;

    // 1. Constructor rỗng (Bắt buộc cho Retrofit/Gson)
    public Task() {
    }

    // 2. Constructor đầy đủ
    public Task(int projectId, String status, String time, String title, String description, String location) {
        this.projectId = projectId;
        this.status = status;
        this.time = time;
        this.title = title;
        this.description = description;
        this.location = location;

    }

    // 3. Getters
    public int getId() { return id; }
    public int getProjectId() { return projectId; }
    public String getStatus() { return status; }
    public String getTime() { return time; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }

    // 4. Setters
    public void setId(int id) { this.id = id; }
    public void setProjectId(int projectId) { this.projectId = projectId; }
    public void setStatus(String status) { this.status = status; }
    public void setTime(String time) { this.time = time; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setLocation(String location) { this.location = location; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

}