package com.example.myapplication.model;

public class Project {

    private int id; // Khóa chính để liên kết với Task
    private int userId;
    private String name;
    private String description;
    private int totalTasks;
    private int finishedTasks;
    private String colorHex;
    private int iconResId;

    // 1. Constructor rỗng (Cực kỳ quan trọng để Retrofit đọc được dữ liệu JSON)
    public Project() {
    }

    // 2. Constructor đầy đủ
    public Project(String name, String description, int totalTasks, int finishedTasks, String colorHex, int iconResId) {
        this.name = name;
        this.description = description;
        this.totalTasks = totalTasks;
        this.finishedTasks = finishedTasks;
        this.colorHex = colorHex;
        this.iconResId = iconResId;
    }

    // 3. Các hàm Getter để lấy dữ liệu (Bao gồm cả getId)
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getTotalTasks() { return totalTasks; }
    public int getFinishedTasks() { return finishedTasks; }
    public String getColorHex() { return colorHex; }
    public int getIconResId() { return iconResId; }

    // 4. Các hàm Setter để gán dữ liệu
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }
    public void setFinishedTasks(int finishedTasks) { this.finishedTasks = finishedTasks; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
    public void setIconResId(int iconResId) { this.iconResId = iconResId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    // 5. Các hàm tính toán tự động
    // Tự động tính toán phần trăm hoàn thành
    public int getProgressPercentage() {
        if (totalTasks == 0) return 0;
        return (int) (((float) finishedTasks / totalTasks) * 100);
    }

    // Tự động tính số công việc còn lại (Đã sửa lại dấu trừ)
    public int getRemainingTasks() {
        return totalTasks - finishedTasks;
    }
}